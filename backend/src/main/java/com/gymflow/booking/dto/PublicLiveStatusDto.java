package com.gymflow.booking.dto;

/**
 * Franja "En directo" de la portada (Trozo 7b, LOGIC.md §9): solo datos calculados de verdad
 * desde la base de datos. Nunca "gente entrenando ahora" ni aforo del gimnasio: no hay control
 * de acceso, así que no hay forma real de calcularlos.
 */
public record PublicLiveStatusDto(int classesRemainingToday, SessionSummary nextClass, int classesWithFreeSlotsNow) {
}
