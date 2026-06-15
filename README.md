# ICTAZ MU Chapter Financial Tracker

> **Purpose:** Educational project & personal tool for managing chapter finances as Treasurer of the ICTAZ Mu Chapter (Information and Communications Technology Association of Zambia, Mulungushi University Chapter).

## Overview

A full-stack financial management system with an **Android client** (Java) and **Python/FastAPI backend** for AI-powered report generation. All transaction data lives on-device using Room (SQLite); the backend is only used for generating intelligent financial reports via OpenRouter AI.

## Architecture

```
┌──────────────────────────────────────────────────┐
│  Android App (Java)                              │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ Room DB  │  │ Work-    │  │ Google Drive  │  │
│  │ (SQLite) │  │ Manager  │  │ Backup/Sync   │  │
│  └──────────┘  └──────────┘  └───────────────┘  │
│       │                                          │
│       ▼ POST /api/generate-report                │
│  ┌──────────────────────────────────────────┐    │
│  │ FastAPI Backend (Python)                 │    │
│  │  ┌─────────┐ ┌──────────┐ ┌──────────┐  │    │
│  │  │Analytics│ │OpenRouter│ │ ReportLab│  │    │
│  │  │Service  │ │ AI       │ │ PDF Gen  │  │    │
│  │  └─────────┘ └──────────┘ └──────────┘  │    │
│  └──────────────────────────────────────────┘    │
└──────────────────────────────────────────────────┘
```

### Key Design Decisions

- **Offline-first** — All transactions stored locally on device. No server-side database.
- **Amounts in ngwee** — Monetary values stored as integers (100 ngwee = 1 ZMW) to avoid floating-point issues.
- **Backend is stateless** — Only computes analytics, calls AI, and generates PDFs. No auth, no database.
- **Incremental backup** — Detects changed transactions by `updatedAt` timestamps to minimize uploads.

## Tech Stack

### Backend
| Component | Technology |
|-----------|-----------|
| Language | Python 3.14 |
| Framework | FastAPI |
| Server | Uvicorn |
| PDF Generation | ReportLab |
| AI Integration | OpenRouter (Qwen 3 70B) |
| Validation | Pydantic |

### Frontend (Android)
| Component | Technology |
|-----------|-----------|
| Language | Java 11 |
| Local DB | Room 2.8.4 (SQLite) |
| Networking | Retrofit 2.9.0 + OkHttp |
| Backup | Google Drive API |
| Auth | Credential Manager, Biometric, PIN |
| Background | WorkManager 2.11.1 |

## Features

- **Transaction Management** — Add, edit, delete income/expenses with categories, payment methods (Cash / Mobile Money), member tracking
- **Dashboard** — Summary cards (income, expenses, balance), recent transactions with color coding
- **Multi-criteria Filtering** — Date range, category, payment method, approval status, member search with chip display
- **Approvals Workflow** — Pending transactions list with approve/reject actions
- **Member View** — Income grouped by member with search
- **Reports (3 tabs)** — Weekly summary, term summary, and AI-generated insights with branded PDF download
- **AI Insights** — Sends transaction data to backend → OpenRouter analyzes financial health → returns executive summary, recommendations, concerns
- **Export** — CSV with optional summary header, local PDF with styled tables
- **Security** — App lock with PIN (4-6 digits), biometric authentication, session timeout (configurable from immediate to 30 min)
- **Google Drive Backup** — Full & incremental backup, restore, auto-backup scheduling
- **Dark Theme** — Full dark mode support

## Setup

### Backend
```bash
cd backend
python -m venv .venv
.venv\Scripts\activate      # Windows
# source .venv/bin/activate # Linux/Mac
pip install -r requirements.txt
cp .env.example .env        # Add your OpenRouter API key
uvicorn main:app --reload   # http://localhost:8000
```

### Android App
1. Open `frontend/` in Android Studio
2. Sync Gradle (AGP 8.12.3)
3. Update `BASE_URL` in `ReportApiService.java` to your backend URL
4. Run on device/emulator (minSdk 24)

## Environment Variables (Backend)

| Variable | Description |
|----------|-------------|
| `OPENROUTER_API_KEY` | Your OpenRouter API key |
| `AI_MODEL` | Model to use (default: `qwen/qwen3-vl-30b-a3b-thinking`) |
| `DEBUG` | Enable debug logging |
| `REPORTS_DIR` | PDF output directory (default: `reports/`) |

## License

Educational use — ICTAZ MU Chapter.
