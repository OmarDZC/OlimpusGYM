package com.gymflow.workout.dto;

import java.util.List;

/**
 * Rutina semanal vigente completa, para "Mi entrenamiento" (Trozo 9d): los 7 días, cada uno
 * con sus ejercicios y el progreso real de series. {@code todayIndex} (0=lunes...6=domingo)
 * para que el cliente resalte el día de hoy sin recalcularlo él mismo.
 */
public record WeekWorkoutResponse(boolean hasPlan, int todayIndex, List<DayProgress> days) {

    public static WeekWorkoutResponse noPlan(int todayIndex) {
        return new WeekWorkoutResponse(false, todayIndex, List.of());
    }
}
