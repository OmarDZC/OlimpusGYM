package com.gymflow.assistant.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiAssistantChatResponse(String reply, @JsonProperty("pending_action") AiPendingAction pendingAction) {
}
