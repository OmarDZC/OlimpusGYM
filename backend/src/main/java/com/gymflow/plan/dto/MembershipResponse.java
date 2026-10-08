package com.gymflow.plan.dto;

import java.time.Instant;

public record MembershipResponse(
    String planCode,
    String planName,
    int priceCents,
    String status,
    Instant startedAt,
    Instant renewsAt,
    boolean aiTrainer,
    boolean aiAssistant) {
}
