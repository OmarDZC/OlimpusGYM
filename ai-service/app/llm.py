"""Cliente de LLM independiente del proveedor.

Usa el SDK de OpenAI apuntando a cualquier endpoint compatible
(Ollama, Groq, Gemini, OpenRouter...). Cambiar de proveedor = cambiar variables de entorno.
"""
from openai import OpenAI

from app.config import settings
from app.llm_retry import REQUEST_TIMEOUT

# Conectar debe ser rápido (si el proveedor no está, que falle enseguida); generar una
# rutina de verdad puede tardar, de ahí la lectura larga. `max_retries=0`: los reintentos
# ante un fallo del proveedor o una respuesta inválida ya los hace `app/llm_retry.py` y
# `app/trainer.py` con el motivo exacto del fallo; dejar también los del SDK (3 por
# defecto) multiplicaba la espera sin ningún beneficio, sobre todo cuando ni siquiera hay
# con quién hablar (antes tardaba ~30 s en dar el 503).


def get_client() -> OpenAI:
    # Ollama no necesita clave, pero el SDK exige un valor no vacío.
    return OpenAI(
        base_url=settings.llm_base_url,
        api_key=settings.llm_api_key or "not-needed",
        timeout=REQUEST_TIMEOUT,
        max_retries=0,
    )
