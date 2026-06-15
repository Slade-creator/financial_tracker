from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    openrouter_api_key: str
    ai_model: str = "qwen/qwen3-vl-30b-a3b-thinking"
    debug: bool = False
    reports_dir: str = "reports"
    max_transactions_per_request: int = 500

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    return Settings()