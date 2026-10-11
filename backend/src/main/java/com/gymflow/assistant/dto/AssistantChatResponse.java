package com.gymflow.assistant.dto;

import java.util.List;

public record AssistantChatResponse(
    String reply, PendingActionDto pendingAction, List<ScheduleOptionDto> options, List<String> contextOptionIds) {
}
