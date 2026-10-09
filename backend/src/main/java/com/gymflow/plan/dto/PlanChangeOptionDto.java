package com.gymflow.plan.dto;

import java.time.Instant;

/**
 * Una de las tarifas disponibles, vista desde "qué pasaría si cambio a esta" (LOGIC.md §12).
 * {@code direction}: CURRENT (la que ya tiene), UPGRADE (precio mayor, se puede subir ya) o
 * DOWNGRADE (precio menor, se programaría para la siguiente renovación).
 */
public record PlanChangeOptionDto(
    String planCode,
    String planName,
    int priceCents,
    String direction,
    Integer amountDueTodayCents,
    Instant effectiveAt) {
}
