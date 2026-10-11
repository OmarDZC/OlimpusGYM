package com.gymflow.ai;

import com.gymflow.plan.PlanCode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Límite de uso de la IA (Trozo 8a-2, LOGIC.md §6/§7): por socio y por día según su tarifa
 * (BÁSICA no tiene IA, así que no le aplica ningún límite: su tarifa simplemente no deja
 * llamar a estos métodos, ver {@code WorkoutService}/{@code AssistantService}), y un tope
 * global diario para toda la aplicación, pensado para no agotar el cupo gratuito del
 * proveedor del LLM con mucho tráfico de visitas a la demo.
 *
 * <p>Contadores en Redis (`INCR` + expiración), uno por socio+función+día y otro global por
 * función+día, para que sobrevivan a reinicios del backend y se compartan si algún día hay
 * más de una instancia. Si Redis falla, la IA debe seguir funcionando igual (mejor dejar
 * pasar una petición de más que tirar la función entera por un Redis caído): se registra un
 * aviso y se deja pasar, nunca se lanza un error al socio por este motivo.
 */
@Component
public class AiUsageLimiter {

    private static final Logger log = LoggerFactory.getLogger(AiUsageLimiter.class);
    private static final ZoneId MEMBER_ZONE = ZoneId.of("Europe/Madrid");
    // Algo más de un día: de sobra para que la clave muera sola aunque haya desfase de reloj,
    // sin dejar basura acumulándose en Redis.
    private static final Duration KEY_TTL = Duration.ofHours(26);

    private final StringRedisTemplate redis;
    private final Clock clock;
    private final int completaRoutinesPerDay;
    private final int completaAssistantMessagesPerDay;
    private final int atletaProRoutinesPerDay;
    private final int atletaProAssistantMessagesPerDay;
    private final int globalDailyCap;

    public AiUsageLimiter(
            StringRedisTemplate redis,
            Clock clock,
            @Value("${gymflow.ai.limits.completa.routines-per-day}") int completaRoutinesPerDay,
            @Value("${gymflow.ai.limits.completa.assistant-messages-per-day}") int completaAssistantMessagesPerDay,
            @Value("${gymflow.ai.limits.atleta-pro.routines-per-day}") int atletaProRoutinesPerDay,
            @Value("${gymflow.ai.limits.atleta-pro.assistant-messages-per-day}") int atletaProAssistantMessagesPerDay,
            @Value("${gymflow.ai.limits.global-daily-cap}") int globalDailyCap) {
        this.redis = redis;
        this.clock = clock;
        this.completaRoutinesPerDay = completaRoutinesPerDay;
        this.completaAssistantMessagesPerDay = completaAssistantMessagesPerDay;
        this.atletaProRoutinesPerDay = atletaProRoutinesPerDay;
        this.atletaProAssistantMessagesPerDay = atletaProAssistantMessagesPerDay;
        this.globalDailyCap = globalDailyCap;
    }

    /**
     * Comprueba el límite y, si se permite, registra el uso. Lanza
     * {@link AiUsageLimitExceededException} si el socio ya agotó su cupo de hoy o si se
     * alcanzó el tope global del día; no lanza nada (deja pasar la petición) si Redis no
     * responde.
     */
    public void checkAndRecordUsage(UUID tenantId, UUID userId, PlanCode planCode, AiFeature feature) {
        int memberLimit = memberLimitFor(planCode, feature);
        String today = clock.instant().atZone(MEMBER_ZONE).toLocalDate().toString();

        try {
            String memberKey = "ai-usage:member:%s:%s:%s:%s".formatted(tenantId, userId, feature, today);
            long memberCount = incrementWithTtl(memberKey);
            if (memberCount > memberLimit) {
                throw new AiUsageLimitExceededException(
                    "Has alcanzado tu límite diario de uso de la IA. Vuelve a intentarlo mañana.");
            }

            // Un único contador para toda la aplicación (rutinas y mensajes juntos, todas las
            // tarifas): es el cupo del proveedor del LLM el que hay que proteger, y ese es uno
            // solo para todo, no uno por función.
            String globalKey = "ai-usage:global:%s".formatted(today);
            long globalCount = incrementWithTtl(globalKey);
            if (globalCount > globalDailyCap) {
                throw new AiUsageLimitExceededException(
                    "La demostración ha alcanzado su límite de IA de hoy, vuelve mañana.");
            }
        } catch (AiUsageLimitExceededException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("No se pudo comprobar el límite de uso de la IA en Redis: la petición se deja pasar.", e);
        }
    }

    private long incrementWithTtl(String key) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, KEY_TTL);
        }
        return count == null ? 0L : count;
    }

    private int memberLimitFor(PlanCode planCode, AiFeature feature) {
        return switch (planCode) {
            case ATLETA_PRO -> feature == AiFeature.ROUTINE ? atletaProRoutinesPerDay : atletaProAssistantMessagesPerDay;
            // COMPLETA es el caso normal; BASICA no tiene acceso a la IA (su tarifa ya lo
            // bloquea antes de llegar aquí), así que un límite de COMPLETA por defecto es
            // inofensivo si alguna vez se llamara con BASICA.
            default -> feature == AiFeature.ROUTINE ? completaRoutinesPerDay : completaAssistantMessagesPerDay;
        };
    }
}
