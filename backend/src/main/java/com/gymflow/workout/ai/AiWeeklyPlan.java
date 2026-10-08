package com.gymflow.workout.ai;

import java.util.List;

/** Respuesta de {@code POST /v1/trainer/plan} del servicio de IA (app/schemas.py::WeeklyPlan). */
public record AiWeeklyPlan(String summary, List<AiDay> days) {
}
