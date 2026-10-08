package com.gymflow.workout.dto;

import java.util.List;

/** Estructura que se serializa a JSON en {@code workout_plans.plan_json}. */
public record WeeklyPlan(List<PlanDay> days) {
}
