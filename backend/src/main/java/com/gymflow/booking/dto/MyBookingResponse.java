package com.gymflow.booking.dto;

import java.time.Instant;
import java.util.UUID;

public record MyBookingResponse(
    UUID id,
    UUID sessionId,
    String sessionName,
    String discipline,
    String room,
    String coachName,
    Instant startsAt,
    Instant endsAt,
    String status,
    Integer waitlistPosition) {
}
