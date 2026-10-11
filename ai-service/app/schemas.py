from enum import Enum

from pydantic import BaseModel, Field


class Goal(str, Enum):
    HYPERTROPHY = "hypertrophy"
    STRENGTH = "strength"
    FAT_LOSS = "fat_loss"
    GENERAL = "general"


class Level(str, Enum):
    BEGINNER = "beginner"
    INTERMEDIATE = "intermediate"
    ADVANCED = "advanced"


class PlanRequest(BaseModel):
    goal: Goal
    level: Level
    days_per_week: int = Field(ge=1, le=7)
    equipment: list[str] = Field(default_factory=list)
    limitations: list[str] = Field(default_factory=list, description="Lesiones o limitaciones")


class ExercisePlan(BaseModel):
    name: str
    sets: int = Field(ge=1, le=10)
    reps: str = Field(description="Ej: '8-12' o '5'")
    rest_seconds: int = Field(ge=0, le=600)
    notes: str | None = None


class DayPlan(BaseModel):
    day: int = Field(ge=1, le=7)
    focus: str
    exercises: list[ExercisePlan]


class WeeklyPlan(BaseModel):
    summary: str
    days: list[DayPlan]


# ---- Asistente de reservas con IA (Trozo 6) ----


class ChatRole(str, Enum):
    USER = "user"
    ASSISTANT = "assistant"


class ChatMessage(BaseModel):
    role: ChatRole
    content: str = Field(min_length=1, max_length=2000)


class PendingActionType(str, Enum):
    BOOK = "book"
    CANCEL = "cancel"


class ScheduleOption(BaseModel):
    """Una clase o reserva real, construida por el servidor a partir de los datos que
    devolvió la herramienta (nunca escrita por el modelo): lo que necesita el frontend para
    pintar una tarjeta sin tener que fiarse de texto libre."""

    id: str
    name: str | None = None
    discipline: str | None = None
    starts_at: str | None = None
    room: str | None = None
    coach_name: str | None = None
    free_slots: int | None = None


class PendingAction(BaseModel):
    """Propuesta de reservar o cancelar, pendiente de que el socio la confirme aparte."""

    type: PendingActionType
    session_id: str | None = None
    booking_id: str | None = None
    summary: str
    option: ScheduleOption | None = None


class AssistantChatRequest(BaseModel):
    """`confirm_action` presente = turno de confirmación: no se habla con el LLM, se ejecuta
    directamente contra el backend (y lo vuelve a validar él, no el modelo).

    `context_option_ids`: los ids de las tarjetas que vio el socio en la respuesta anterior
    (ver `AssistantChatResponse.context_option_ids`), reenviados tal cual por el cliente. Si el
    último mensaje es una afirmación corta ("sí", "vale", "ok") y hay exactamente un id, se
    trata como elegir esa opción sin hablar con el modelo (ver `_converse`)."""

    messages: list[ChatMessage] = Field(default_factory=list, max_length=20)
    member_token: str = Field(min_length=10)
    confirm_action: PendingAction | None = None
    context_option_ids: list[str] = Field(default_factory=list)


class AssistantChatResponse(BaseModel):
    reply: str
    pending_action: PendingAction | None = None
    options: list[ScheduleOption] | None = None
    # Ids de las clases/reservas que acaban de mostrarse (tarjetas de `options`), para que el
    # cliente los reenvíe en `AssistantChatRequest.context_option_ids` en el siguiente mensaje.
    context_option_ids: list[str] = Field(default_factory=list)
