package com.gymflow.booking.dto;

import java.time.Instant;
import java.util.UUID;

/** LOGIC.md §6: GET /public/schedule. "Plazas libres" nunca es negativo. */
public record SessionSummary(
    UUID id,
    String name,
    String discipline,
    String room,
    String coachName,
    Instant startsAt,
    Instant endsAt,
    int capacity,
    int bookedCount,
    int freeSlots,
    int waitlistCount,
    String status) {
}
