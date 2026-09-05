"""Tests for services/ai_service.generate_insights with httpx mocked out.

Covers:
- the ```json markdown fence-stripping logic (ai_service.py ~lines 52-55)
- the malformed-response -> RuntimeError path
- the HTTP error -> RuntimeError path

No network access: httpx.AsyncClient is replaced by a fake context-manager
client whose post() returns a canned OpenRouter-shaped response.
"""

import asyncio
import json

import httpx
import pytest

import services.ai_service as ai_service
from services.ai_service import generate_insights

OPENROUTER_URL = ai_service.OPENROUTER_URL

VALID_INSIGHTS = {
    "executiveSummary": "The association is profitable.",
    "insights": ["Fees are the main income source"],
    "recommendations": ["Reduce transport costs"],
    "concerns": "Low savings rate",
}


class FakeResponse:
    """OpenRouter-shaped JSON response."""

    def __init__(self, content):
        self._content = content
        self.status_code = 200
        self.text = ""

    def raise_for_status(self):
        pass

    def json(self):
        return {"choices": [{"message": {"content": self._content}}]}


class ErrorResponse:
    """Simulates a non-2xx response whose raise_for_status() raises."""

    def __init__(self, status_code):
        self.status_code = status_code
        self.text = "server exploded"
        self._raw = httpx.Response(
            status_code,
            request=httpx.Request("POST", OPENROUTER_URL),
        )

    def raise_for_status(self):
        self._raw.raise_for_status()

    def json(self):  # pragma: no cover - never reached
        return {}


class FakeAsyncClient:
    """Stands in for httpx.AsyncClient as an async context manager."""

    def __init__(self, response, **kwargs):
        self._response = response
        self.requests = []

    async def __aenter__(self):
        return self

    async def __aexit__(self, *exc_info):
        return False

    async def post(self, url, **kwargs):
        self.requests.append((url, kwargs))
        return self._response


@pytest.fixture
def install_client(monkeypatch):
    """Returns a helper that swaps in a FakeAsyncClient returning `response`."""

    def _install(response):
        client = FakeAsyncClient(response)
        monkeypatch.setattr(ai_service.httpx, "AsyncClient", lambda **kw: client)
        return client

    return _install


def run(prompt="Analyze this"):
    return asyncio.run(generate_insights(prompt))


class TestFenceStripping:
    def test_plain_json_response(self, install_client):
        install_client(FakeResponse(json.dumps(VALID_INSIGHTS)))
        result = run()
        assert result.executiveSummary == "The association is profitable."
        assert result.insights == ["Fees are the main income source"]
        assert result.recommendations == ["Reduce transport costs"]
        assert result.concerns == "Low savings rate"

    def test_json_code_fence_with_language_tag(self, install_client):
        content = "```json\n" + json.dumps(VALID_INSIGHTS) + "\n```"
        install_client(FakeResponse(content))
        result = run()
        assert result.executiveSummary == "The association is profitable."
        assert result.insights == ["Fees are the main income source"]

    def test_code_fence_without_language_tag(self, install_client):
        content = "```\n" + json.dumps(VALID_INSIGHTS) + "\n```"
        install_client(FakeResponse(content))
        result = run()
        assert result.executiveSummary == "The association is profitable."

    def test_unclosed_code_fence(self, install_client):
        # Model outputs ```json{...} with no closing fence
        content = "```json" + json.dumps(VALID_INSIGHTS)
        install_client(FakeResponse(content))
        result = run()
        assert result.recommendations == ["Reduce transport costs"]

    def test_fence_with_surrounding_whitespace(self, install_client):
        content = "\n  ```json\n" + json.dumps(VALID_INSIGHTS) + "\n```  \n"
        install_client(FakeResponse(content))
        result = run()
        assert result.concerns == "Low savings rate"


class TestDefaultsForPartialJson:
    def test_missing_fields_use_defaults(self, install_client):
        install_client(FakeResponse(json.dumps({"executiveSummary": "Only this"})))
        result = run()
        assert result.executiveSummary == "Only this"
        assert result.insights == []
        assert result.recommendations == []
        assert result.concerns == "No significant concerns."


class TestErrorPaths:
    def test_malformed_json_raises_runtime_error(self, install_client):
        install_client(FakeResponse("this is {not valid json"))
        with pytest.raises(RuntimeError, match="AI returned malformed response"):
            run()

    def test_malformed_json_inside_fence_raises(self, install_client):
        install_client(FakeResponse("```json\n{oops\n```"))
        with pytest.raises(RuntimeError, match="AI returned malformed response"):
            run()

    def test_missing_choices_key_raises_runtime_error(self, install_client):
        # KeyError on data["choices"] must be converted to RuntimeError
        response = FakeResponse(json.dumps(VALID_INSIGHTS))

        def broken_json():
            return {"error": "rate limited"}

        response.json = broken_json
        install_client(response)
        with pytest.raises(RuntimeError, match="AI returned malformed response"):
            run()

    def test_http_error_raises_runtime_error(self, install_client):
        install_client(ErrorResponse(500))
        with pytest.raises(RuntimeError, match="AI service error: 500"):
            run()


class TestRequestShape:
    def test_sends_openrouter_payload(self, install_client):
        client = install_client(FakeResponse(json.dumps(VALID_INSIGHTS)))
        run("my prompt")

        url, kwargs = client.requests[0]
        assert url == OPENROUTER_URL
        payload = kwargs["json"]
        assert payload["messages"][0]["role"] == "system"
        assert payload["messages"][1] == {"role": "user", "content": "my prompt"}
        assert "Authorization" in kwargs["headers"]
