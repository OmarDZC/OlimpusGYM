from fastapi import FastAPI, HTTPException

from app.config import settings
from app.schemas import PlanRequest, WeeklyPlan

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
def generate_plan(request: PlanRequest) -> WeeklyPlan:
    """Genera una rutina semanal. Pendiente de implementar (Fase 3 del roadmap).

    Idea: llamar al LLM configurado (cliente OpenAI-compatible) pidiendo JSON,
    validarlo contra WeeklyPlan con Pydantic y reintentar si no es válido.
    """
    raise HTTPException(status_code=501, detail="Generación de rutinas pendiente (Fase 3)")
