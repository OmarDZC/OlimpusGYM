package com.gymflow.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gymflow.plan.PlanCode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Límite de uso de la IA (Trozo 8a-2, LOGIC.md §6/§7), contra un Redis real de Testcontainers
 * y con un reloj controlado: por socio y por día, se reinicia al día siguiente, el tope
 * global para toda la aplicación, y que un Redis caído no rompe la petición. Se omite
 * automáticamente si no hay Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
class AiUsageLimiterTest {

    @Container
    static GenericContainer<?> redis =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private Clock clock;
    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        clock = Mockito.mock(Clock.class);
        Mockito.when(clock.instant()).thenReturn(Instant.parse("2026-10-11T10:00:00Z"));

        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        // Cada test empieza con Redis vacío: las claves son por día, y varios tests usan el
        // mismo "hoy" simulado, así que se pisarían entre sí si no se limpia antes.
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    private AiUsageLimiter limiter(int completaRoutines, int completaAssistant, int atletaProRoutines,
            int atletaProAssistant, int globalCap) {
        return new AiUsageLimiter(
            redisTemplate, clock, completaRoutines, completaAssistant, atletaProRoutines, atletaProAssistant,
            globalCap);
    }

    @Test
    void allowsUsesUpToTheMemberLimitAndThenBlocksWithAClearMessage() {
        AiUsageLimiter limiter = limiter(2, 3, 5, 6, 1000);
        UUID tenantId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        limiter.checkAndRecordUsage(tenantId, memberId, PlanCode.COMPLETA, AiFeature.ROUTINE);
        limiter.checkAndRecordUsage(tenantId, memberId, PlanCode.COMPLETA, AiFeature.ROUTINE);

        AiUsageLimitExceededException ex = assertThrows(AiUsageLimitExceededException.class,
            () -> limiter.checkAndRecordUsage(tenantId, memberId, PlanCode.COMPLETA, AiFeature.ROUTINE));
        assertTrue(ex.getMessage().toLowerCase().contains("límite"));
    }

    @Test
    void theLimitIsPerMemberNotSharedWithOtherMembers() {
        AiUsageLimiter limiter = limiter(1, 3, 5, 6, 1000);
        UUID tenantId = UUID.randomUUID();
        UUID memberA = UUID.randomUUID();
        UUID memberB = UUID.randomUUID();

        limiter.checkAndRecordUsage(tenantId, memberA, PlanCode.COMPLETA, AiFeature.ROUTINE);
        assertThrows(AiUsageLimitExceededException.class,
            () -> limiter.checkAndRecordUsage(tenantId, memberA, PlanCode.COMPLETA, AiFeature.ROUTINE));

        // El socio B, con su propio cupo de hoy sin tocar, puede generar la suya.
        assertDoesNotThrow(() -> limiter.checkAndRecordUsage(tenantId, memberB, PlanCode.COMPLETA, AiFeature.ROUTINE));
    }

    @Test
    void atletaProHasAMoreGenerousLimitThanCompleta() {
        AiUsageLimiter limiter = limiter(1, 1, 5, 6, 1000);
        UUID tenantId = UUID.randomUUID();
        UUID member = UUID.randomUUID();

        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> limiter.checkAndRecordUsage(tenantId, member, PlanCode.ATLETA_PRO, AiFeature.ROUTINE));
        }
        assertThrows(AiUsageLimitExceededException.class,
            () -> limiter.checkAndRecordUsage(tenantId, member, PlanCode.ATLETA_PRO, AiFeature.ROUTINE));
    }

    @Test
    void theLimitResetsTheNextDay() {
        AiUsageLimiter limiter = limiter(1, 3, 5, 6, 1000);
        UUID tenantId = UUID.randomUUID();
        UUID member = UUID.randomUUID();

        limiter.checkAndRecordUsage(tenantId, member, PlanCode.COMPLETA, AiFeature.ROUTINE);
        assertThrows(AiUsageLimitExceededException.class,
            () -> limiter.checkAndRecordUsage(tenantId, member, PlanCode.COMPLETA, AiFeature.ROUTINE));

        // Al día siguiente (misma hora, un día más), el cupo vuelve a estar entero.
        Mockito.when(clock.instant()).thenReturn(Instant.parse("2026-10-12T10:00:00Z"));
        assertDoesNotThrow(() -> limiter.checkAndRecordUsage(tenantId, member, PlanCode.COMPLETA, AiFeature.ROUTINE));
    }

    @Test
    void theGlobalDailyCapBlocksEvenDifferentMembersOnceReached() {
        // Límites por socio muy holgados para que solo el tope global pueda bloquear aquí.
        AiUsageLimiter limiter = limiter(1000, 1000, 1000, 1000, 3);
        UUID tenantId = UUID.randomUUID();

        limiter.checkAndRecordUsage(tenantId, UUID.randomUUID(), PlanCode.COMPLETA, AiFeature.ROUTINE);
        limiter.checkAndRecordUsage(tenantId, UUID.randomUUID(), PlanCode.COMPLETA, AiFeature.ASSISTANT_MESSAGE);
        limiter.checkAndRecordUsage(tenantId, UUID.randomUUID(), PlanCode.ATLETA_PRO, AiFeature.ROUTINE);

        AiUsageLimitExceededException ex = assertThrows(AiUsageLimitExceededException.class,
            () -> limiter.checkAndRecordUsage(tenantId, UUID.randomUUID(), PlanCode.ATLETA_PRO, AiFeature.ASSISTANT_MESSAGE));
        assertTrue(ex.getMessage().contains("demostración"));
    }

    @Test
    void aRedisFailureLetsTheRequestThroughInsteadOfBreakingIt() {
        LettuceConnectionFactory brokenFactory = new LettuceConnectionFactory("localhost", 1);
        brokenFactory.afterPropertiesSet();
        StringRedisTemplate brokenRedis = new StringRedisTemplate(brokenFactory);
        brokenRedis.afterPropertiesSet();
        AiUsageLimiter limiter = new AiUsageLimiter(brokenRedis, clock, 1, 1, 1, 1, 1);

        assertDoesNotThrow(() ->
            limiter.checkAndRecordUsage(UUID.randomUUID(), UUID.randomUUID(), PlanCode.COMPLETA, AiFeature.ROUTINE));

        brokenFactory.destroy();
    }
}
