package com.gymflow.assistant.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Forma exacta de `ScheduleOption` en `ai-service/app/schemas.py` (snake_case). */
public record AiScheduleOption(
    String id,
    String name,
    String discipline,
    @JsonProperty("starts_at") String startsAt,
    String room,
    @JsonProperty("coach_name") String coachName,
    @JsonProperty("free_slots") Integer freeSlots) {
}
