from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    database_url: str = "postgresql://gymflow:gymflow@localhost:5432/gymflow"
    backend_base_url: str = "http://localhost:8080"

    # Proveedor de LLM: cualquier API compatible con OpenAI.
    # Por defecto Ollama en local (gratis, sin límites, sin enviar datos fuera).
    llm_base_url: str = "http://localhost:11434/v1"
    llm_api_key: str = ""
    llm_model: str = "llama3.1:8b"

    # Proveedor de respaldo, opcional (Trozo 9e): si el principal agota sus reintentos con un
    # 429/5xx, se prueba una vez aquí antes de rendirse (ver app/llm_retry.py). Vacío por
    # defecto = mismo comportamiento que antes, solo el principal.
    llm_fallback_base_url: str = ""
    llm_fallback_api_key: str = ""
    llm_fallback_model: str = ""


settings = Settings()
