package com.gymflow.assistant.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Forma exacta de `PendingAction` en `ai-service/app/schemas.py` (snake_case). */
public record AiPendingAction(
    String type,
    @JsonProperty("session_id") String sessionId,
    @JsonProperty("booking_id") String bookingId,
    String summary,
    AiScheduleOption option) {
}
