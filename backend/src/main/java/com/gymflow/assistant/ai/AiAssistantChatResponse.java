package com.gymflow.assistant.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiAssistantChatResponse(
    String reply,
    @JsonProperty("pending_action") AiPendingAction pendingAction,
    List<AiScheduleOption> options) {
}
