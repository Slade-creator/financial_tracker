# Developer Documentation

Welcome! This folder contains the developer documentation for the **ICTAZ MU Chapter Financial Tracker** — a full-stack financial management system with an Android client (Java) and a Python/FastAPI backend for AI-powered report generation.

## Documentation Index

| Document | Description |
|----------|-------------|
| [Architecture](./ARCHITECTURE.md) | System overview, Android app layering (MVVM), backend request flow |
| [Data Model](./DATA_MODEL.md) | Room database schema (Chen ER notation), entities, and DAO queries |
| [API Reference](./API.md) | Backend endpoints, request/response formats, error codes |
| [Screens & Navigation](./SCREENS.md) | Every screen, nav graph walkthrough, UI conventions |
| [Backup & Sync](./BACKUP.md) | Google Drive backup, incremental diff/merge, auto-backup scheduling |
| [Security](./SECURITY.md) | App lock, PIN storage (Keystore + AES-GCM), session timeout |
| [Contributing](./CONTRIBUTING.md) | Local setup (uv for backend), project conventions, and workflow |

## Quick Start

### Backend (Python/FastAPI — managed with [uv](https://docs.astral.sh/uv/))

```bash
# Install uv (Windows PowerShell)
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"

cd backend
uv sync                      # creates .venv and installs deps from uv.lock
cp .env.example .env         # Add your OpenRouter API key
uv run uvicorn main:app --reload   # http://localhost:8000
```

### Android App

1. Open `frontend/` in Android Studio
2. Sync Gradle
3. Add `BACKEND_BASE_URL=https://your-backend.example.com/` to `frontend/local.properties`
   (defaults to `http://10.0.2.2:8000/` on the emulator)
4. Run on device/emulator (minSdk 24)

## Where Things Live

```
financialtracker/
├── frontend/     ← Android app (Java, Room, Retrofit)
├── backend/      ← FastAPI AI report service (Python)
├── docs/         ← You are here
└── reports/      ← Generated PDFs (runtime output)
```
