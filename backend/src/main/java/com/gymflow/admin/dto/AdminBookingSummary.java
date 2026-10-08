package com.gymflow.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminBookingSummary(
    UUID bookingId,
    UUID userId,
    String memberName,
    String sessionName,
    Instant sessionStartsAt,
    String status,
    Integer waitlistPosition) {
}
