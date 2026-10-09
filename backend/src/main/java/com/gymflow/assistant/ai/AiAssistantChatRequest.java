package com.gymflow.assistant.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Cuerpo de {@code POST /v1/assistant/chat} del servicio de IA (LOGIC.md §7). */
public record AiAssistantChatRequest(
    List<AiChatMessage> messages,
    @JsonProperty("member_token") String memberToken,
    @JsonProperty("confirm_action") AiPendingAction confirmAction) {
}
