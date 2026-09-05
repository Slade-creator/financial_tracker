from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    openrouter_api_key: str
    ai_model: str = "qwen/qwen3-vl-30b-a3b-thinking"
    # Public URL sent to OpenRouter as HTTP-Referer (attribution/rankings).
    # Optional — when empty the header is simply omitted.
    http_referer: str = ""
    debug: bool = False
    # Server bind settings for `python main.py` (imported app is unaffected).
    host: str = "0.0.0.0"
    port: int = 8000
    reports_dir: str = "reports"
    # Origins allowed to call this API from a browser (CORS).
    # In .env this must be a JSON array, e.g.:
    #   ALLOWED_ORIGINS=["http://localhost:5173", "http://10.0.2.2:8000"]
    # Empty list (default) means no cross-origin browser access.
    allowed_origins: list[str] = []
    max_transactions_per_request: int = 500

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    return Settings()