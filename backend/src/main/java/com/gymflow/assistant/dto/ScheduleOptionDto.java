package com.gymflow.assistant.dto;

import java.time.Instant;
import java.util.UUID;

/** Una clase o reserva real, para que el frontend pinte una tarjeta sin fiarse de texto libre. */
public record ScheduleOptionDto(
    UUID id,
    String name,
    String discipline,
    Instant startsAt,
    String room,
    String coachName,
    Integer freeSlots) {
}
