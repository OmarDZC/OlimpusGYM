package com.gymflow.assistant.ai;

import com.gymflow.assistant.dto.AssistantChatRequest;
import com.gymflow.assistant.dto.AssistantChatResponse;
import com.gymflow.assistant.dto.ChatMessageDto;
import com.gymflow.assistant.dto.PendingActionDto;
import com.gymflow.assistant.dto.ScheduleOptionDto;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final Pattern DETAIL_PATTERN = Pattern.compile("\"detail\"\\s*:\\s*\"([^\"]*)\"");
    private static final String DEFAULT_RATE_LIMIT_MESSAGE =
            "El asistente ha alcanzado su límite de peticiones por ahora. Espera un momento y vuelve a intentarlo.";

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
                toAiMessages(request.messages()),
                memberToken,
                toAiAction(request.confirmAction()),
                request.contextOptionIds() != null ? request.contextOptionIds() : List.of());
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
            if (e instanceof HttpStatusCodeException httpError) {
                int status = httpError.getStatusCode().value();
                if (status == 429) {
                    throw new AssistantRateLimitedException(extractDetail(httpError));
                }
                if (status == 503) {
                    throw new AssistantUnavailableException();
                }
            }
            throw new AssistantFailedException();
        }
    }

    /** El cuerpo del 429 del servicio de IA es {@code {"detail": "mensaje en español"}}: se
     * extrae ese mensaje para mostrárselo tal cual al socio, sin inventar uno genérico si el
     * servicio ya dio uno claro. */
    private String extractDetail(HttpStatusCodeException httpError) {
        String body = httpError.getResponseBodyAsString();
        Matcher matcher = DETAIL_PATTERN.matcher(body);
        return matcher.find() ? matcher.group(1) : DEFAULT_RATE_LIMIT_MESSAGE;
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
                action.summary(),
                toAiOption(action.option()));
    }

    private AiScheduleOption toAiOption(ScheduleOptionDto option) {
        if (option == null) {
            return null;
        }
        return new AiScheduleOption(
                option.id() != null ? option.id().toString() : null,
                option.name(),
                option.discipline(),
                option.startsAt() != null ? option.startsAt().toString() : null,
                option.room(),
                option.coachName(),
                option.freeSlots());
    }

    private AssistantChatResponse toResponse(AiAssistantChatResponse response) {
        if (response == null) {
            throw new AssistantFailedException();
        }
        List<ScheduleOptionDto> options = response.options() == null
                ? null
                : response.options().stream().map(this::toOptionDto).toList();
        List<String> contextOptionIds = response.contextOptionIds() != null ? response.contextOptionIds() : List.of();
        return new AssistantChatResponse(response.reply(), toDto(response.pendingAction()), options, contextOptionIds);
    }

    private PendingActionDto toDto(AiPendingAction action) {
        if (action == null) {
            return null;
        }
        return new PendingActionDto(
                action.type(),
                action.sessionId() != null ? UUID.fromString(action.sessionId()) : null,
                action.bookingId() != null ? UUID.fromString(action.bookingId()) : null,
                action.summary(),
                toOptionDto(action.option()));
    }

    private ScheduleOptionDto toOptionDto(AiScheduleOption option) {
        if (option == null) {
            return null;
        }
        return new ScheduleOptionDto(
                option.id() != null ? UUID.fromString(option.id()) : null,
                option.name(),
                option.discipline(),
                option.startsAt() != null ? Instant.parse(option.startsAt()) : null,
                option.room(),
                option.coachName(),
                option.freeSlots());
    }
}
