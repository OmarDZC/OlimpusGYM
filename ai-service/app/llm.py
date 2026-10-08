"""Cliente de LLM independiente del proveedor.

Usa el SDK de OpenAI apuntando a cualquier endpoint compatible
(Ollama, Groq, Gemini, OpenRouter...). Cambiar de proveedor = cambiar variables de entorno.
"""
from openai import OpenAI

from app.config import settings


def get_client() -> OpenAI:
    # Ollama no necesita clave, pero el SDK exige un valor no vacío.
    return OpenAI(base_url=settings.llm_base_url, api_key=settings.llm_api_key or "not-needed")
