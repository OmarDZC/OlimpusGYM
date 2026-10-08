package com.gymflow.admin.dto;

import java.time.Instant;
import java.util.UUID;

/** PATCH parcial: cualquier campo nulo deja el valor actual de la sesión sin tocar. */
public record UpdateSessionRequest(
    String name,
    String discipline,
    String description,
    UUID coachUserId,
    String room,
    Instant startsAt,
    Instant endsAt,
    Integer capacity) {
}
