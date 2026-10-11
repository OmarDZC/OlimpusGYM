package com.gymflow.assistant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * {@code confirmAction} presente = turno de confirmación: no se habla con el LLM, se ejecuta
 * directamente (y lo vuelve a validar igual de estricto el backend, no el modelo).
 */
public record AssistantChatRequest(
    @NotNull @Size(max = 20, message = "Demasiados mensajes en la conversación") List<@Valid ChatMessageDto> messages,
    @Valid PendingActionDto confirmAction,
    List<String> contextOptionIds) {
}
