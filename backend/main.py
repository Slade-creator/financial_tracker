import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import os

from routers import reports
from config import get_settings

logging.basicConfig(level=logging.DEBUG if get_settings().debug else logging.INFO,
                    format='%(asctime)s - %(name)s - %(levelname)-8s - %(message)s')
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    os.makedirs(settings.reports_dir, exist_ok=True)
    logger.info(f"Reports directory: {os.path.abspath(settings.reports_dir)}")
    logger.info(f"AI model: {settings.ai_model}")
    yield
    logger.info("Shutting down.")


app = FastAPI(
    title="ICTAZ MU Financial Tracker — AI Backend",
    description=(
        "FastAPI backend for the ICTAZ MU Chapter Android financial tracker. "
        "Generates AI-powered PDF reports via OpenRouter (Llama 3 70B)."
    ),
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

app.include_router(reports.router)

if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=int(os.getenv("PORT", 8000)),
        reload=True  # Enable auto-reload during development
    )
