package com.gymflow.plan.dto;

import java.time.Instant;

public record MembershipResponse(
    String planCode,
    String planName,
    int priceCents,
    /** NULL = reservas ilimitadas. */
    Integer classesPerMonth,
    String status,
    Instant startedAt,
    Instant periodStart,
    Instant periodEnd,
    boolean aiTrainer,
    boolean aiAssistant,
    /** Bajada de tarifa programada para la siguiente renovación, o NULL si no hay ninguna. */
    String pendingPlanCode,
    String pendingPlanName) {
}
