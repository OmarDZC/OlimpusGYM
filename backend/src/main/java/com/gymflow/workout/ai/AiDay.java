package com.gymflow.workout.ai;

import java.util.List;

/** {@code day}: 1 = lunes ... 7 = domingo (app/schemas.py::DayPlan). */
public record AiDay(int day, String focus, List<AiExercise> exercises) {
}
