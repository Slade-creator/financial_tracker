"""API tests for routers/reports.py via FastAPI TestClient.

generate_insights is mocked (no real OpenRouter call), and pdf_service.generate
is stubbed so no PDF is written to disk during tests.
"""

import os

import pytest
from fastapi.testclient import TestClient

import main
from config import get_settings
from Model.report import AIInsights
import routers.reports as reports_router


@pytest.fixture
def client():
    # TestClient without a context manager: the app lifespan (which creates the
    # reports directory and logs settings) does not run — not needed here.
    return TestClient(main.app)


@pytest.fixture
def mocked_pipeline(monkeypatch):
    """Stub out the AI call and the PDF writer; capture what they receive."""
    captured = {}

    async def fake_generate_insights(prompt):
        captured["prompt"] = prompt
        return AIInsights(
            executiveSummary="Healthy finances.",
            insights=["Income is stable"],
            recommendations=["Keep it up"],
            concerns="No significant concerns.",
        )

    def fake_generate_pdf(**kwargs):
        captured["analytics"] = kwargs["analytics"]
        return "reports/test-fake.pdf"

    monkeypatch.setattr(reports_router, "generate_insights", fake_generate_insights)
    monkeypatch.setattr(reports_router.pdf_service, "generate", fake_generate_pdf)
    return captured


def txn(tid="t1", ttype="income", amount=10000, approved=1):
    return {
        "id": tid,
        "transactionType": ttype,
        "amount": amount,
        "category": "Fees",
        "paymentMethod": "CASH",
        "isApproved": approved,
        "transactionDate": "2026-03-15T10:00:00Z",
        "createdAt": "2026-03-15T09:00:00Z",
        "updatedAt": "2026-03-15T09:00:00Z",
    }


class TestValidation:
    def test_empty_transaction_list_returns_400(self, client):
        resp = client.post("/api/generate-report", json={
            "period": "March 2026",
            "reportType": "monthly",
            "transactions": [],
        })
        assert resp.status_code == 400
        assert resp.json()["detail"] == "No transactions provided"

    def test_too_many_transactions_returns_400(self, client):
        max_allowed = get_settings().max_transactions_per_request
        resp = client.post("/api/generate-report", json={
            "period": "March 2026",
            "reportType": "monthly",
            "transactions": [txn(f"t{i}") for i in range(max_allowed + 1)],
        })
        assert resp.status_code == 400
        assert "Too many transactions" in resp.json()["detail"]

    def test_missing_period_returns_422(self, client):
        resp = client.post("/api/generate-report", json={
            "reportType": "monthly",
            "transactions": [txn()],
        })
        assert resp.status_code == 422

    def test_invalid_transaction_field_returns_422(self, client):
        bad = txn()
        bad["amount"] = "not-an-int"  # amount must be int (ngwee)
        resp = client.post("/api/generate-report", json={
            "period": "March 2026",
            "transactions": [bad],
        })
        assert resp.status_code == 422


class TestHappyPath:
    def test_generate_report_success(self, client, mocked_pipeline):
        resp = client.post("/api/generate-report", json={
            "period": "March 2026",
            "reportType": "monthly",
            "transactions": [
                txn("i1", "income", 50000),
                txn("e1", "expense", 20000),
                txn("p1", "expense", 10000, approved=0),
            ],
        })
        assert resp.status_code == 200
        body = resp.json()

        assert body["success"] is True
        assert body["reportUrl"] == "reports/test-fake.pdf"
        assert body["insights"]["executiveSummary"] == "Healthy finances."
        assert body["insights"]["insights"] == ["Income is stable"]
        assert body["generatedAt"]

        # Summary line reflects analytics (pending txn excluded from totals)
        assert "Income: ZMW 500.00" in body["summary"]
        assert "Expenses: ZMW 200.00" in body["summary"]
        assert "Net: ZMW 300.00 (surplus)" in body["summary"]
        assert "Savings rate: 60.0%" in body["summary"]

        # Prompt handed to the AI contains the period and type
        assert "March 2026" in mocked_pipeline["prompt"]
        assert "monthly" in mocked_pipeline["prompt"]

    def test_generate_report_deficit_summary(self, client, mocked_pipeline):
        resp = client.post("/api/generate-report", json={
            "period": "Q1",
            "reportType": "quarterly",
            "transactions": [
                txn("i1", "income", 10000),
                txn("e1", "expense", 30000),
            ],
        })
        assert resp.status_code == 200
        assert "Net: ZMW -200.00 (deficit)" in resp.json()["summary"]


class TestHealth:
    def test_health_check(self, client):
        resp = client.get("/api/health")
        assert resp.status_code == 200
        assert resp.json()["status"] == "ok"

    def test_download_missing_report_returns_404(self, client):
        resp = client.get("/reports/does-not-exist.pdf")
        assert resp.status_code == 404


class TestReportDownload:
    @pytest.fixture
    def reports_dir(self, tmp_path, monkeypatch):
        """Point settings.reports_dir at an isolated temp directory."""
        monkeypatch.setattr(get_settings(), "reports_dir", str(tmp_path))
        return tmp_path

    def _write_pdf(self, reports_dir, name):
        path = reports_dir / name
        path.write_bytes(b"%PDF-1.4\n%fake-test-pdf\n")
        return path

    def test_legitimate_pdf_downloads(self, client, reports_dir):
        self._write_pdf(reports_dir, "monthly-report.pdf")
        resp = client.get("/reports/monthly-report.pdf")
        assert resp.status_code == 200
        assert resp.headers["content-type"] == "application/pdf"
        assert resp.content.startswith(b"%PDF-1.4")

    def test_traversal_with_parent_dir_returns_404(self, client, reports_dir):
        # Place a secret PDF OUTSIDE the reports dir, in the tmp_path parent's
        # sibling hierarchy (simulating a file elsewhere on disk).
        self._write_pdf(reports_dir.parent, "secret.pdf")
        resp = client.get("/reports/../secret.pdf")
        assert resp.status_code == 404

    def test_traversal_with_percent_encoded_slash_returns_404(self, client, reports_dir):
        self._write_pdf(reports_dir.parent, "secret.pdf")
        # "..%2Fsecret.pdf" decodes server-side to "../secret.pdf"
        resp = client.get("/reports/..%2Fsecret.pdf")
        assert resp.status_code == 404

    def test_windows_backslash_traversal_returns_404(self, client, reports_dir):
        self._write_pdf(reports_dir.parent, "secret.pdf")
        resp = client.get("/reports/..\\secret.pdf")
        assert resp.status_code == 404

    def test_absolute_style_name_returns_404(self, client, reports_dir):
        self._write_pdf(reports_dir.parent, "secret.pdf")
        resp = client.get("/reports//secret.pdf")
        assert resp.status_code == 404

    def test_subdir_escape_inside_reports_dir_returns_404(self, client, reports_dir):
        # File exists within reports_dir but under a subdirectory — the
        # filename must be exactly a bare <name>.pdf.
        subdir = reports_dir / "nested"
        subdir.mkdir()
        (subdir / "inner.pdf").write_bytes(b"%PDF-1.4\n")
        resp = client.get("/reports/nested%2Finner.pdf")
        assert resp.status_code == 404

    def test_non_pdf_extension_returns_404(self, client, reports_dir):
        (reports_dir / "notes.txt").write_text("not a pdf")
        resp = client.get("/reports/notes.txt")
        assert resp.status_code == 404

    def test_dotdot_substring_in_name_returns_404(self, client, reports_dir):
        # Any filename containing ".." (even a legal-looking one like
        # "..pdf") is rejected outright — belt-and-braces traversal guard.
        self._write_pdf(reports_dir, "..pdf")
        resp = client.get("/reports/..pdf")
        assert resp.status_code == 404
