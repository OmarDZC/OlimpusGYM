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
