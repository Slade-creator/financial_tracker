import json
import logging

import httpx
from app.Model.report import AIInsights
from config import get_settings

logger = logging.getLogger(__name__)

OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"

async def generate_insights(prompt: str) -> AIInsights:

    settings = get_settings()

    headers = {
        "Authorization": f"Bearer {settings.openrouter_api_key}",
        "Content-Type": "application/json",
        "HTTP-Referer": "https://ictaz-mu-tracker.onrender.com",
        "X-Title": "ICTAZ MU Financial Tracker",
    }

    payload = {
        "model": settings.ai_model,
        "messages": [
            {
                "role": "system",
                "content": (
                    "You are a financial analyst. "
                    "Always respond with valid JSON only. "
                    "No markdown, no code blocks, no explanations."
                ),
            },
            {"role": "user", "content": prompt},
        ],
        "temperature": 0.3,
        "max_tokens": 600,
    }

    try:
        async with httpx.AsyncClient(timeout=45.0) as client:
            response = await client.post(
                OPENROUTER_URL, headers=headers, json=payload
            )
            response.raise_for_status()

        data = response.json()
        raw_text = data["choices"][0]["message"]["content"].strip()

        logger.debug(f"Raw AI response: {raw_text[:300]}")

        if raw_text.startswith("```"):
            raw_text = raw_text.split("```")[1]
            if raw_text.startswith("json"):
                raw_text = raw_text[4:]

        parsed = json.loads(raw_text)

        return AIInsights(
            executiveSummary=parsed.get("executiveSummary", "Report generated successfully."),
            insights=parsed.get("insights", []),
            recommendations=parsed.get("recommendations", []),
            concerns=parsed.get("concerns", "No significant concerns."),
        )

    except httpx.HTTPStatusError as e:
        logger.error(f"OpenRouter HTTP error {e.response.status_code}: {e.response.text}")
        raise RuntimeError(f"AI service error: {e.response.status_code}")
    except (json.JSONDecodeError, KeyError) as e:
        logger.error(f"Failed to parse AI response: {e}")
        raise RuntimeError("AI returned malformed response")
    except Exception as e:
        logger.error(f"Unexpected AI error: {e}")
        raise RuntimeError(f"AI service unavailable: {str(e)}")



