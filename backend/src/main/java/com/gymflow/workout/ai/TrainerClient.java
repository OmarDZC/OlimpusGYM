package com.gymflow.workout.ai;

import com.gymflow.workout.dto.GenerateWorkoutRequest;
import java.time.Duration;
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

/** Llama a {@code POST /v1/trainer/plan} del servicio de IA (LOGIC.md §7). */
@Component
public class TrainerClient {

    private static final Pattern DETAIL_PATTERN = Pattern.compile("\"detail\"\\s*:\\s*\"([^\"]*)\"");
    private static final String DEFAULT_RATE_LIMIT_MESSAGE =
            "El servicio de IA está saturado, inténtalo de nuevo en un minuto.";

    private final RestClient restClient;

    public TrainerClient(@Value("${gymflow.ai.base-url}") String baseUrl) {
        // Una rutina real puede tardar: timeout de lectura generoso para no cortar al LLM
        // a mitad de respuesta, pero con límite para no dejar la petición colgada para siempre.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(90));

        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public AiWeeklyPlan generatePlan(GenerateWorkoutRequest request) {
        AiPlanRequest body = new AiPlanRequest(
                request.goal(), request.level(), request.daysPerWeek(), request.equipment(), request.limitations());
        try {
            return restClient.post()
                    .uri("/v1/trainer/plan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(AiWeeklyPlan.class);
        } catch (ResourceAccessException e) {
            // No se pudo ni conectar (servicio caído, timeout): mismo trato que un 503 suyo.
            throw new TrainerUnavailableException();
        } catch (RestClientException e) {
            if (e instanceof HttpStatusCodeException httpError) {
                int status = httpError.getStatusCode().value();
                if (status == 429) {
                    throw new TrainerRateLimitedException(extractDetail(httpError));
                }
                if (status == 503) {
                    throw new TrainerUnavailableException();
                }
            }
            throw new TrainerFailedException();
        }
    }

    /** El cuerpo del 429 del servicio de IA es {@code {"detail": "mensaje en español"}}: se
     * extrae ese mensaje para mostrárselo tal cual al socio. */
    private String extractDetail(HttpStatusCodeException httpError) {
        String body = httpError.getResponseBodyAsString();
        Matcher matcher = DETAIL_PATTERN.matcher(body);
        return matcher.find() ? matcher.group(1) : DEFAULT_RATE_LIMIT_MESSAGE;
    }
}
