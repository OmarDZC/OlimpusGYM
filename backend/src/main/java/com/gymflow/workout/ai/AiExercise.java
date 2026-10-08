package com.gymflow.workout.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

/** app/schemas.py::ExercisePlan. */
public record AiExercise(
    String name, int sets, String reps, @JsonProperty("rest_seconds") int restSeconds, String notes) {
}
