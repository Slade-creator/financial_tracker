# ICTAZ MU Financial Tracker — AI Backend

FastAPI backend for the Android financial tracker app.  
Generates AI-powered PDF reports via OpenRouter (model configurable via
`AI_MODEL`, default `qwen/qwen3-vl-30b-a3b-thinking`).

---

## Stack

| Layer      | Technology                        |
|------------|-----------------------------------|
| API        | FastAPI + Uvicorn                 |
| AI         | OpenRouter (Qwen by default)      |
| PDF        | ReportLab                         |
| Hosting    | Render.com (free tier)            |
| Android    | Retrofit 2 + OkHttp               |

---

## Local Setup

The backend uses **[uv](https://docs.astral.sh/uv/)** for dependency management. `pyproject.toml` declares dependencies; `uv.lock` pins exact versions.

```bash
# 1. Install uv (skip if already installed)
#    Windows (PowerShell):
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"
#    macOS/Linux:
#    curl -LsSf https://astral.sh/uv/install.sh | sh

# 2. Enter the project
cd backend

# 3. Install Python 3.14 (pinned in .python-version) if missing
uv python install

# 4. Create .venv and install all dependencies from uv.lock (incl. dev group)
uv sync

# 5. Configure environment
cp .env.example .env
# Edit .env and paste your OpenRouter key

# 6. Run
uv run uvicorn main:app --reload
# → http://localhost:8000
# → http://localhost:8000/docs  (Swagger UI)

# Tests
uv run pytest
```

Useful commands: `uv add <pkg>` / `uv add --dev <pkg>` (adds to `pyproject.toml` **and** `uv.lock`), `uv sync --no-dev` (production install), `uv lock --upgrade` (re-resolve versions).

> ⚠️ Avoid `pip install` directly into `.venv` — it desyncs `uv.lock`. To reset: delete `.venv` and run `uv sync`.

---

## Get Your OpenRouter API Key

1. Go to https://openrouter.ai and sign up
2. Click **Keys** → **Create Key**
3. Copy the key (starts with `sk-or-v1-...`)
4. Paste it into `.env` as `OPENROUTER_API_KEY`

Many models on OpenRouter have a **free tier** — no credit card needed to start.

---

## Deploy to Render.com

1. Push this folder to a GitHub repo
2. Go to https://render.com → **New Web Service**
3. Connect your GitHub repo
4. Render auto-detects `render.yaml` — no manual config needed
5. Add one **environment variable** in the Render dashboard:
   - Key: `OPENROUTER_API_KEY`
   - Value: your key from OpenRouter
6. Click **Deploy**

Your live URL will be: `https://your-app-name.onrender.com`

---

## Android Wiring

The backend URL is injected into the app as a `BuildConfig` field. Add it to
`frontend/local.properties` (not committed):

```properties
# Development (emulator) — this is the default if omitted
BACKEND_BASE_URL=http://10.0.2.2:8000/

# Production (after Render deploy)
BACKEND_BASE_URL=https://your-app-name.onrender.com/
```

No Java source changes are needed; the value is read by
`ReportApiService.java` via `BuildConfig.BACKEND_BASE_URL`.

---

## API Endpoints

| Method | Path                    | Description                        |
|--------|-------------------------|------------------------------------|
| POST   | `/api/generate-report`  | Generate AI report + PDF           |
| GET    | `/api/health`           | Health check (used by Android)     |
| GET    | `/reports/{filename}`   | Download generated PDF             |
| GET    | `/docs`                 | Swagger UI (dev only)              |

### POST `/api/generate-report`

**Request** (matches `ReportRequest.java`):
```json
{
  "period": "2025-03-01 to 2025-03-31",
  "reportType": "monthly",
  "transactions": [
    {
      "id": "abc-123",
      "transactionType": "income",
      "amount": 50000,
      "memberName": "Alice Banda",
      "category": "Membership Fees",
      "paymentMethod": "Mobile Money",
      "isApproved": 1,
      "transactionDate": "2025-03-01",
      "notes": null,
      "createdAt": "2025-03-01T08:00:00",
      "updatedAt": "2025-03-01T08:00:00"
    }
  ]
}
```

> **Note:** `amount` is in **ngwee** (integer). ZMW 500.00 = `50000`.

**Response** (matches `ReportResponse.java`):
```json
{
  "success": true,
  "reportUrl": "/reports/report_abc123def4.pdf",
  "summary": "Monthly report for 2025-03-01 to 2025-03-31. Income: ZMW 500.00 ...",
  "insights": {
    "executiveSummary": "...",
    "insights": ["...", "..."],
    "recommendations": ["...", "..."],
    "concerns": "..."
  },
  "generatedAt": "2025-03-31T14:22:00+00:00"
}
```

To download the PDF, call:
```
GET https://your-app.onrender.com/reports/report_abc123def4.pdf
```

---

## Project Structure

```
backend/
├── main.py               ← FastAPI app + CORS
├── config.py             ← Settings from .env
├── Model/
│   ├── transaction.py    ← Mirrors Android Transaction.java
│   └── report.py         ← Mirrors ReportRequest/Response/AIInsights
├── services/
│   ├── analytics.py      ← Data crunching + AI prompt builder
│   ├── ai_service.py     ← OpenRouter chat-completions call
│   └── pdf_service.py    ← ReportLab PDF generation
├── routers/
│   └── reports.py        ← /api/generate-report, /api/health
├── reports/              ← Generated PDFs (auto-created)
├── requirements.txt
├── pyproject.toml
└── .env.example
```

---

## Cost Estimate (Render free tier + OpenRouter free tier)

| Usage           | AI Cost       | Hosting   | Total/month |
|-----------------|---------------|-----------|-------------|
| Dev / testing   | Free          | Free      | **$0**      |
| 100 reports/mo  | ~$0.02        | Free      | **~$0.02**  |
| 500 reports/mo  | ~$0.10        | Free      | **~$0.10**  |

Pricing depends on the model set in `AI_MODEL` — many OpenRouter models offer
a free tier.  
A typical report prompt uses ~400 tokens in + ~200 tokens out ≈ **$0.0004 per report**.
