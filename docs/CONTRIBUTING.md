# Contributing

Thanks for helping out! This guide covers getting the project running locally, the conventions used in the codebase, and the workflow for making changes.

## Prerequisites

| Tool | Version | For |
|------|---------|-----|
| Android Studio | Recent (AGP 8.12.3) | Android app |
| JDK | 11+ | Android app |
| [uv](https://docs.astral.sh/uv/) | latest | Backend (package & environment manager) |

> The backend uses **uv**, not raw pip. `backend/pyproject.toml` declares dependencies and `backend/uv.lock` pins the exact versions — always use `uv` commands so the lockfile stays authoritative. Python **3.14** is pinned via `backend/.python-version`; uv will download it automatically if missing.

## Project Setup

### 1. Backend (uv)

Install uv first if you don't have it:

```bash
# Windows (PowerShell)
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"

# macOS / Linux
curl -LsSf https://astral.sh/uv/install.sh | sh
```

Then set up and run the backend:

```bash
cd backend

# Install Python 3.14 (pinned in .python-version) if missing
uv python install

# Create .venv and install ALL dependencies from uv.lock (includes pytest dev group)
uv sync

# Configure environment
cp .env.example .env    # paste your OpenRouter API key

# Run the dev server
uv run uvicorn main:app --reload
```

Useful uv commands:

| Command | What it does |
|---------|--------------|
| `uv sync` | Create/update `.venv` exactly per `uv.lock` (adds dev group by default) |
| `uv sync --no-dev` | Production install, dev dependencies excluded |
| `uv run <cmd>` | Run a command inside the project's virtual environment |
| `uv add <pkg>` | Add a runtime dependency (updates `pyproject.toml` **and** `uv.lock`) |
| `uv add --dev <pkg>` | Add a dev-only dependency (goes to the `[dependency-groups].dev` group) |
| `uv remove <pkg>` | Remove a dependency |
| `uv lock --upgrade` | Re-resolve and upgrade all locked versions |

> ⚠️ Don't `pip install` into the venv directly — that desyncs `uv.lock`. If you did, `rm -rf .venv && uv sync` to reset.

Verify at `http://localhost:8000/docs` (Swagger UI) and `http://localhost:8000/api/health`.

### 2. Android App

1. Open `frontend/` in Android Studio and let Gradle sync
2. Create `frontend/local.properties` entries (file is git-ignored):

   ```properties
   BACKEND_BASE_URL=http://10.0.2.2:8000/
   GOOGLE_DRIVE_CLIENT_ID=your-client-id.apps.googleusercontent.com
   ```

   `10.0.2.2` is the emulator's alias for your machine's localhost. Both values are injected as `BuildConfig` fields by `app/build.gradle.kts`.
3. Run on an emulator or device (minSdk 24)

### 3. Running Tests

```bash
# Backend (pytest via uv)
cd backend
uv run pytest

# Android unit tests
cd frontend
.\gradlew.bat test
```

Backend tests live in `backend/tests/` (`test_analytics.py`, `test_ai_service.py`, `test_reports_api.py`); Android tests in `frontend/app/src/test/java/`.

---

## Codebase Orientation

- **Android (Java)** — `frontend/app/src/main/java/com/studentassoc/financialtracker/`
  - MVVM: `View/` → `ViewModel/` → `Repository/` → `DTO/TransactionDao` → Room
  - Cross-cutting: `services/` (backup, Retrofit API, exporters), `Security/`, `Backup/` (WorkManager), `Utils/`
- **Backend (Python/FastAPI)** — `backend/`
  - `main.py` (app + CORS) → `routers/reports.py` (endpoints) → `services/` (analytics, AI, PDF) → `Model/` (pydantic schemas)
- **Docs** — this folder (`docs/`). Update docs whenever you change behaviour that they describe.

See [Architecture](./ARCHITECTURE.md) for diagrams and the full layer breakdown.

---

## Conventions

### Android (Java)

- Follow the existing MVVM boundaries: **fragments never touch the DAO directly** — go through `TransactionViewModel`.
- New DB queries go in `TransactionDao`; new projections go in `Model/`.
- Amounts are **integers in ngwee** — never use `float`/`double` for money.
- Room entity ↔ backend pydantic models must stay in sync (`Transaction.java` ↔ `backend/Model/transaction.py`). Change both sides together.
- Dates are ISO `yyyy-MM-dd` strings — range queries rely on lexicographic ordering.
- Database is version 1 with destructive downgrade migration. If you bump the schema version, write a real `Migration` — don't leave destructive fallbacks in place.

### Backend (Python)

- All configuration via pydantic-settings in `config.py` — don't read `os.environ` ad hoc.
- Keep the router thin: validation → services. Business logic belongs in `services/`.
- Preserve the path-traversal defences in `GET /reports/{filename}` — validate before joining paths and re-verify the resolved path.
- Never commit `.env` or log the API key.

### Documentation

- All diagrams use **Mermaid** (rendered natively by GitHub and most viewers). Keep diagrams next to the text that explains them.
- Update the relevant doc in the same PR as the code change:
  - New/changed endpoint → `docs/API.md`
  - Schema/DAO changes → `docs/DATA_MODEL.md`
  - New components or flows → `docs/ARCHITECTURE.md`

---

## Workflow

1. Create a branch for your change
2. Make the change + tests
3. Run backend `pytest` and Android `gradlew test`
4. Update docs if behaviour changed
5. Open a PR describing what changed and why

## Reporting Issues

Include: what you did, what you expected, what happened, and (for backend issues) the relevant log lines from uvicorn. For AI-related failures, note the `AI_MODEL` in use and whether the OpenRouter key had quota.
