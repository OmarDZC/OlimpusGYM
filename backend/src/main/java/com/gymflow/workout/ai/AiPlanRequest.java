package com.gymflow.workout.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Cuerpo de {@code POST /v1/trainer/plan} del servicio de IA (LOGIC.md §7). */
public record AiPlanRequest(
    String goal,
    String level,
    @JsonProperty("days_per_week") int daysPerWeek,
    List<String> equipment,
    List<String> limitations) {
}
