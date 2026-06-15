# ICTAZ MU Financial Tracker — AI Backend

FastAPI backend for the Android financial tracker app.  
Generates AI-powered PDF reports using **Llama 3 70B** via OpenRouter.

---

## Stack

| Layer      | Technology                        |
|------------|-----------------------------------|
| API        | FastAPI + Uvicorn                 |
| AI         | Llama 3 70B via OpenRouter        |
| PDF        | ReportLab                         |
| Hosting    | Render.com (free tier)            |
| Android    | Retrofit 2 + OkHttp               |

---

## Local Setup

```bash
# 1. Clone / enter the project
cd financial-tracker-backend

# 2. Create virtual environment
python3 -m venv venv
source venv/bin/activate       # Windows: venv\Scripts\activate

# 3. Install dependencies
pip install -r requirements.txt

# 4. Configure environment
cp .env.example .env
# Edit .env and paste your OpenRouter key

# 5. Run
uvicorn app.main:app --reload
# → http://localhost:8000
# → http://localhost:8000/docs  (Swagger UI)
```

---

## Get Your OpenRouter API Key

1. Go to https://openrouter.ai and sign up
2. Click **Keys** → **Create Key**
3. Copy the key (starts with `sk-or-v1-...`)
4. Paste it into `.env` as `OPENROUTER_API_KEY`

Llama 3 70B has a **free tier** — no credit card needed to start.

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

After deploying, update **one line** in `ReportApiService.java`:

```java
// Development (emulator)
private static final String BASE_URL = "http://10.0.2.2:8000";

// Production (after Render deploy)
private static final String BASE_URL = "https://your-app-name.onrender.com";
```

> **Tip:** Use a `BuildConfig` flag to switch automatically:
> ```java
> private static final String BASE_URL = BuildConfig.DEBUG
>     ? "http://10.0.2.2:8000"
>     : "https://your-app-name.onrender.com";
> ```

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
financial-tracker-backend/
├── app/
│   ├── main.py               ← FastAPI app + CORS
│   ├── config.py             ← Settings from .env
│   ├── models/
│   │   ├── transaction.py    ← Mirrors Android Transaction.java
│   │   └── report.py         ← Mirrors ReportRequest/Response/AIInsights
│   ├── services/
│   │   ├── analytics_service.py  ← Data crunching + AI prompt builder
│   │   ├── ai_service.py         ← OpenRouter / Llama 3 70B call
│   │   └── pdf_service.py        ← ReportLab PDF generation
│   └── routers/
│       └── reports.py        ← /api/generate-report, /api/health
├── reports/                  ← Generated PDFs (auto-created)
├── requirements.txt
├── render.yaml               ← Render.com deploy config
└── .env.example
```

---

## Cost Estimate (Render free tier + OpenRouter free tier)

| Usage           | AI Cost       | Hosting   | Total/month |
|-----------------|---------------|-----------|-------------|
| Dev / testing   | Free          | Free      | **$0**      |
| 100 reports/mo  | ~$0.02        | Free      | **~$0.02**  |
| 500 reports/mo  | ~$0.10        | Free      | **~$0.10**  |

Llama 3 70B on OpenRouter: ~$0.59 per 1M tokens.  
A typical report prompt uses ~400 tokens in + ~200 tokens out ≈ **$0.0004 per report**.