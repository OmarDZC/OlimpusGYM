from fastapi import Depends, FastAPI, HTTPException
from openai import OpenAI

from app.config import settings
from app.llm import get_client
from app.schemas import PlanRequest, WeeklyPlan
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
