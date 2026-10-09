from fastapi import Depends, FastAPI, HTTPException
from openai import OpenAI

from app.assistant import AssistantFailedError, AssistantUnavailableError
from app.assistant import chat as run_assistant_chat
from app.config import settings
from app.gymflow_client import GymflowClient, get_gymflow_client
from app.llm import get_client
from app.schemas import AssistantChatRequest, AssistantChatResponse, PlanRequest, WeeklyPlan
from app.trainer import LlmInvalidOutputError, LlmUnavailableError
from app.trainer import generate_plan as run_trainer

app = FastAPI(title="GymFlow AI Service", version="0.1.0")


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
        return run_trainer(client, request)
    except LlmUnavailableError:
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
    except AssistantUnavailableError:
        raise HTTPException(
            status_code=503,
            detail="El asistente con IA no está disponible ahora mismo. Inténtalo de nuevo en unos minutos.",
        )
    except AssistantFailedError:
        raise HTTPException(
            status_code=502,
            detail="No se pudo completar la conversación esta vez. Inténtalo de nuevo.",
        )
