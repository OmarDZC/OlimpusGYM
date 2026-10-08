package com.gymflow.workout.dto;

import java.util.List;

/**
 * {@code hasPlan=false}: el socio no tiene rutina esta semana (p. ej. su tarifa no incluye
 * entrenador con IA, o todavía no se le ha asignado ninguna; eso se genera en el Trozo 5).
 * {@code restDay=true}: sí tiene rutina, pero hoy toca descanso.
 * {@code dayIndex} se repite aquí (0=lunes...6=domingo) para que el cliente pueda marcar
 * series sin tener que recalcular "qué día es hoy" por su cuenta.
 */
public record TodayWorkoutResponse(
    boolean hasPlan, boolean restDay, int dayIndex, String dayLabel, String focus, List<ExerciseProgress> exercises) {

    public static TodayWorkoutResponse noPlan(int dayIndex) {
        return new TodayWorkoutResponse(false, false, dayIndex, null, null, List.of());
    }

    public static TodayWorkoutResponse rest(int dayIndex, String dayLabel) {
        return new TodayWorkoutResponse(true, true, dayIndex, dayLabel, null, List.of());
    }
}
