package com.gymflow.workout.dto;

import java.util.List;

/**
 * {@code hasPlan=false}: el socio no tiene rutina esta semana (p. ej. su tarifa no incluye
 * entrenador con IA, o todavía no se le ha asignado ninguna; eso se genera en el Trozo 5).
 * {@code restDay=true}: sí tiene rutina, pero hoy toca descanso; {@code nextLabel}/
 * {@code nextFocus} dicen qué toca el próximo día de entrenamiento (Trozo 9d: antes el
 * descanso no decía nada de lo que seguía).
 * {@code dayIndex} se repite aquí (0=lunes...6=domingo) para que el cliente pueda marcar
 * series sin tener que recalcular "qué día es hoy" por su cuenta.
 */
public record TodayWorkoutResponse(
    boolean hasPlan,
    boolean restDay,
    int dayIndex,
    String dayLabel,
    String focus,
    List<ExerciseProgress> exercises,
    String nextLabel,
    String nextFocus) {

    public static TodayWorkoutResponse noPlan(int dayIndex) {
        return new TodayWorkoutResponse(false, false, dayIndex, null, null, List.of(), null, null);
    }

    public static TodayWorkoutResponse rest(int dayIndex, String dayLabel, String nextLabel, String nextFocus) {
        return new TodayWorkoutResponse(true, true, dayIndex, dayLabel, null, List.of(), nextLabel, nextFocus);
    }
}
