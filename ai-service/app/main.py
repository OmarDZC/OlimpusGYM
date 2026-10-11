import logging

from fastapi import Depends, FastAPI, HTTPException
from openai import OpenAI

from app.assistant import AssistantFailedError, AssistantRateLimitedError, AssistantUnavailableError
from app.assistant import chat as run_assistant_chat
from app.config import settings
from app.gymflow_client import GymflowClient, get_gymflow_client
from app.llm import get_client
from app.schemas import AssistantChatRequest, AssistantChatResponse, PlanRequest, WeeklyPlan
from app.trainer import LlmInvalidOutputError, LlmRateLimitedError, LlmUnavailableError
from app.trainer import generate_plan as run_trainer

logger = logging.getLogger(__name__)

app = FastAPI(title="GymFlow AI Service", version="0.1.0")


def _log_provider_failure(source: str, exc: Exception) -> None:
    """Deja rastro de un fallo del proveedor de LLM antes de convertirlo en 503: sin esto, en
    los logs solo se veía el 503 final, sin ninguna pista de si era la clave, el modelo, la red
    o un límite de peticiones (Trozo 7b/9c). Solo el tipo de error y, si el proveedor lo dio, su
    código HTTP: nunca la clave ni las cabeceras de autorización, que `str(exc)`/el traceback de
    las excepciones del SDK de OpenAI no incluyen (vienen del cuerpo de la respuesta, no de la
    petición)."""
    cause = exc.__cause__ or exc
    status_code = getattr(cause, "status_code", None)
    logger.exception(
        "Fallo del proveedor de LLM en %s: tipo=%s status_code=%s",
        source,
        type(cause).__name__,
        status_code,
    )


@app.get("/health")
def health() -> dict[str, object]:
    return {
        "service": "gymflow-ai",
        "status": "ok",
        "llm_base_url": settings.llm_base_url,
        "model": settings.llm_model,
    }


@app.post("/v1/trainer/plan", response_model=WeeklyPlan)
def trainer_plan(request: PlanRequest, client: OpenAI = Depends(get_client)) -> WeeklyPlan:
    try:
        return run_trainer(client, settings.llm_model, request)
    except LlmRateLimitedError as exc:
        # Subclase de LlmUnavailableError: tiene que comprobarse antes. No es que el
        # entrenador esté caído, es que se agotó el cupo de peticiones del proveedor por ahora.
        _log_provider_failure("el entrenador", exc)
        raise HTTPException(
            status_code=429,
            detail="El servicio de IA está saturado, inténtalo de nuevo en un minuto.",
        )
    except LlmUnavailableError as exc:
        _log_provider_failure("el entrenador", exc)
        raise HTTPException(
            status_code=503,
            detail="El entrenador con IA no está disponible ahora mismo. Inténtalo de nuevo en unos minutos.",
        )
    except LlmInvalidOutputError:
        raise HTTPException(
            status_code=502,
            detail="No se pudo generar tu rutina esta vez. Inténtalo de nuevo en unos minutos.",
        )


@app.post("/v1/assistant/chat", response_model=AssistantChatResponse)
def assistant_chat(
    request: AssistantChatRequest,
    client: OpenAI = Depends(get_client),
    gymflow: GymflowClient = Depends(get_gymflow_client),
) -> AssistantChatResponse:
    try:
        return run_assistant_chat(client, gymflow, request)
    except AssistantRateLimitedError as exc:
        # Subclase de AssistantUnavailableError: tiene que comprobarse antes. No es que el
        # asistente esté caído, es que se agotó el cupo de peticiones del proveedor por ahora.
        _log_provider_failure("el asistente", exc)
        raise HTTPException(
            status_code=429,
            detail="El asistente ha alcanzado su límite de peticiones por ahora. Espera un momento y vuelve a intentarlo.",
        )
    except AssistantUnavailableError as exc:
        _log_provider_failure("el asistente", exc)
        raise HTTPException(
            status_code=503,
            detail="El asistente con IA no está disponible ahora mismo. Inténtalo de nuevo en unos minutos.",
        )
    except AssistantFailedError:
        raise HTTPException(
            status_code=502,
            detail="No se pudo completar la conversación esta vez. Inténtalo de nuevo.",
        )
