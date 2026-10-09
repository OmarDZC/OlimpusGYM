package com.gymflow.assistant.ai;

import com.gymflow.assistant.dto.AssistantChatRequest;
import com.gymflow.assistant.dto.AssistantChatResponse;
import com.gymflow.assistant.dto.ChatMessageDto;
import com.gymflow.assistant.dto.PendingActionDto;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Llama a {@code POST /v1/assistant/chat} del servicio de IA (LOGIC.md §7). */
@Component
public class AssistantClient {

    private final RestClient restClient;

    public AssistantClient(@Value("${gymflow.ai.base-url}") String baseUrl) {
        // Igual que TrainerClient: conectar debe ser rápido, pero una conversación con
        // herramientas puede encadenar varias llamadas al LLM y tardar.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(90));

        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public AssistantChatResponse chat(String memberToken, AssistantChatRequest request) {
        AiAssistantChatRequest body = new AiAssistantChatRequest(
                toAiMessages(request.messages()), memberToken, toAiAction(request.confirmAction()));
        try {
            AiAssistantChatResponse response = restClient.post()
                    .uri("/v1/assistant/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(AiAssistantChatResponse.class);
            return toResponse(response);
        } catch (ResourceAccessException e) {
            throw new AssistantUnavailableException();
        } catch (RestClientException e) {
            if (e instanceof HttpStatusCodeException httpError && httpError.getStatusCode().value() == 503) {
                throw new AssistantUnavailableException();
            }
            throw new AssistantFailedException();
        }
    }

    private List<AiChatMessage> toAiMessages(List<ChatMessageDto> messages) {
        return messages.stream().map(m -> new AiChatMessage(m.role(), m.content())).toList();
    }

    private AiPendingAction toAiAction(PendingActionDto action) {
        if (action == null) {
            return null;
        }
        return new AiPendingAction(
                action.type(),
                action.sessionId() != null ? action.sessionId().toString() : null,
                action.bookingId() != null ? action.bookingId().toString() : null,
                action.summary());
    }

    private AssistantChatResponse toResponse(AiAssistantChatResponse response) {
        if (response == null) {
            throw new AssistantFailedException();
        }
        return new AssistantChatResponse(response.reply(), toDto(response.pendingAction()));
    }

    private PendingActionDto toDto(AiPendingAction action) {
        if (action == null) {
            return null;
        }
        return new PendingActionDto(
                action.type(),
                action.sessionId() != null ? UUID.fromString(action.sessionId()) : null,
                action.bookingId() != null ? UUID.fromString(action.bookingId()) : null,
                action.summary());
    }
}
