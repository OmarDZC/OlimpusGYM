"""Reintentos ante 429/5xx/red y proveedor de respaldo, compartidos entre el entrenador y el
asistente (Trozo 9e): la misma lógica en un solo sitio, para no mantenerla duplicada ni
arriesgarse a que se desincronice entre los dos.

Proveedor de respaldo (LLM_FALLBACK_BASE_URL/API_KEY/MODEL, opcionales): si el principal agota
sus reintentos con un fallo que tiene sentido reintentar en otro sitio (429 o 5xx, nunca un
401/400, que es un problema de configuración y daría el mismo error en cualquier proveedor),
se prueba UNA vez en el de respaldo antes de rendirse. Sin esas variables, el comportamiento
es exactamente el de antes (solo el principal). Nunca se registra ninguna clave: de las
excepciones del SDK solo se guarda el tipo y el status_code, igual que ya hacía el asistente.
"""
from __future__ import annotations

import logging
import random
import time as time_module

import httpx
from openai import APIConnectionError, APIStatusError, OpenAI

from app.config import settings

logger = logging.getLogger(__name__)

RETRY_DELAYS_S = (1.0, 3.0)
MAX_RETRY_WAIT_S = 8.0
REQUEST_TIMEOUT = httpx.Timeout(connect=3.0, read=90.0, write=10.0, pool=3.0)


def is_retryable(exc: Exception) -> bool:
    """429 (límite de peticiones) y 5xx (fallo del proveedor) merecen reintentarse; un 4xx
    como 401 o 400 es un problema de configuración o de la petición, no transitorio."""
    if isinstance(exc, APIStatusError):
        return exc.status_code == 429 or exc.status_code >= 500
    return isinstance(exc, APIConnectionError)  # red caída, timeout...


def is_rate_limited(exc: Exception) -> bool:
    return isinstance(exc, APIStatusError) and exc.status_code == 429


def retry_after_seconds(exc: Exception) -> float | None:
    response = getattr(exc, "response", None)
    retry_after = response.headers.get("retry-after") if response is not None else None
    if not retry_after:
        return None
    try:
        return float(retry_after)
    except ValueError:
        return None


def has_fallback() -> bool:
    return bool(settings.llm_fallback_base_url)


def fallback_client() -> OpenAI:
    return OpenAI(
        base_url=settings.llm_fallback_base_url,
        api_key=settings.llm_fallback_api_key or "not-needed",
        timeout=REQUEST_TIMEOUT,
        max_retries=0,
    )


def call_with_retry_and_fallback(primary_client, primary_model: str, **create_kwargs):
    """Llama a `chat.completions.create` en el proveedor principal, con reintentos cortos
    ante 429/5xx/red (hasta MAX_RETRY_WAIT_S de espera total, respetando Retry-After si el
    proveedor lo da). Si se agotan y hay un proveedor de respaldo configurado, prueba una vez
    ahí con su propio modelo antes de propagar el fallo original."""
    budget_left = MAX_RETRY_WAIT_S
    last_exc: Exception | None = None

    for attempt in range(len(RETRY_DELAYS_S) + 1):
        try:
            return primary_client.chat.completions.create(model=primary_model, **create_kwargs)
        except Exception as exc:  # cualquier fallo al hablar con el proveedor: red, 401, timeout...
            last_exc = exc
            retryable = attempt < len(RETRY_DELAYS_S) and budget_left > 0 and is_retryable(exc)
            if not retryable:
                break
            delay = RETRY_DELAYS_S[attempt]
            after = retry_after_seconds(exc)
            if after is not None:
                delay = max(delay, after)
            delay = min(delay + random.uniform(0, 0.5), budget_left)
            logger.warning(
                "Fallo transitorio del proveedor principal (intento %s, %s): reintento en %.1fs",
                attempt + 1,
                type(exc).__name__,
                delay,
            )
            time_module.sleep(delay)
            budget_left -= delay

    if last_exc is not None and is_retryable(last_exc) and has_fallback():
        logger.warning(
            "Proveedor principal agotado (%s); probando una vez con el de respaldo.",
            type(last_exc).__name__,
        )
        try:
            return fallback_client().chat.completions.create(model=settings.llm_fallback_model, **create_kwargs)
        except Exception as fallback_exc:
            logger.warning("El proveedor de respaldo también falló (%s).", type(fallback_exc).__name__)
            raise fallback_exc from last_exc

    raise last_exc
