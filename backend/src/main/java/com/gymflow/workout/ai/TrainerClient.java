package com.gymflow.workout.ai;

import com.gymflow.workout.dto.GenerateWorkoutRequest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Llama a {@code POST /v1/trainer/plan} del servicio de IA (LOGIC.md §7). */
@Component
public class TrainerClient {

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
            int status = extractStatus(e);
            if (status == 503) {
                throw new TrainerUnavailableException();
            }
            throw new TrainerFailedException();
        }
    }

    private int extractStatus(RestClientException e) {
        if (e instanceof org.springframework.web.client.HttpStatusCodeException httpError) {
            return httpError.getStatusCode().value();
        }
        return 0;
    }
}
