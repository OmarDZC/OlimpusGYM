package com.gymflow.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.auth.Role;
import com.gymflow.auth.User;
import com.gymflow.auth.UserRepository;
import com.gymflow.auth.dto.LoginRequest;
import com.gymflow.plan.dto.PlanChangeRequest;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Cambio de tarifa del socio (Trozo 9b, LOGIC.md §12): subir al instante con prorrateo, bajar
 * programado a la siguiente renovación (y poder deshacerlo), y que la renovación diaria aplique
 * lo programado. Reloj controlado (@MockitoBean Clock) para que el periodo y el prorrateo sean
 * deterministas. Se omite automáticamente si no hay Docker disponible.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class MembershipPlanChangeHttpTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TenantRepository tenants;

    @Autowired
    UserRepository users;

    @Autowired
    PlanRepository plans;

    @Autowired
    MembershipRepository memberships;

    @Autowired
    MembershipPlanChangeRepository changes;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    MembershipRenewalScheduler renewalScheduler;

    @MockitoBean
    MembershipNotifier notifier;

    @MockitoBean
    Clock clock;

    private Tenant tenant;
    private Plan basica;
    private Plan completa;
    private Plan atletaPro;
    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

    @BeforeEach
    void setUp() {
        tenant = tenants.findBySlug(Tenant.DEFAULT_SLUG)
            .orElseGet(() -> tenants.save(new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG)));
        basica = plans.findByTenantIdAndCode(tenant.getId(), PlanCode.BASICA)
            .orElseGet(() -> plans.save(new Plan(tenant.getId(), PlanCode.BASICA, "Básica", 3900, 4, 7, false, false)));
        completa = plans.findByTenantIdAndCode(tenant.getId(), PlanCode.COMPLETA)
            .orElseGet(() -> plans.save(new Plan(tenant.getId(), PlanCode.COMPLETA, "Completa", 5900, null, 14, true, true)));
        atletaPro = plans.findByTenantIdAndCode(tenant.getId(), PlanCode.ATLETA_PRO)
            .orElseGet(() -> plans.save(new Plan(tenant.getId(), PlanCode.ATLETA_PRO, "Atleta Pro", 7900, null, 30, true, true)));
        Mockito.when(clock.instant()).thenReturn(NOW);
        Mockito.reset(notifier);
    }

    private User createMemberWithMembership(String email, Plan plan, Instant periodStart) {
        User user = new User(tenant.getId(), email, passwordEncoder.encode("ClaveDePrueba123!"), "Socio Prueba", Role.MEMBER);
        users.save(user);
        memberships.save(new Membership(tenant.getId(), user.getId(), plan.getId(), MembershipStatus.ACTIVE, periodStart));
        return user;
    }

    private String loginAndGetToken(String email) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(email, "ClaveDePrueba123!"));
        String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void upgradingNowChargesTheProratedDifferenceAndRecordsTheAudit() throws Exception {
        // Periodo de 2026-10-01 a 2026-11-01, "ahora" es el 9 (8 días consumidos, 23 restantes).
        Instant periodStart = Instant.parse("2026-10-01T12:00:00Z");
        User user = createMemberWithMembership("sube.tarifa@test.local", completa, periodStart);
        String token = loginAndGetToken("sube.tarifa@test.local");

        String body = objectMapper.writeValueAsString(new PlanChangeRequest(PlanCode.ATLETA_PRO));
        mvc.perform(post("/api/v1/me/membership/upgrade").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.planCode").value("ATLETA_PRO"))
            .andExpect(jsonPath("$.aiAssistant").value(true));

        Membership membership = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenant.getId(), user.getId()).orElseThrow();
        assertEquals(atletaPro.getId(), membership.getPlanId());
        assertEquals(periodStart, membership.getPeriodStart(), "subir no reinicia el periodo");

        List<MembershipPlanChange> audit = changes.findByMembershipIdOrderByCreatedAtDesc(membership.getId());
        assertEquals(1, audit.size());
        assertEquals(PlanChangeType.UPGRADE, audit.get(0).getChangeType());
        assertEquals(user.getId(), audit.get(0).getChangedByUserId());
        assertTrue(audit.get(0).getAmountCents() > 0 && audit.get(0).getAmountCents() < (7900 - 5900));
    }

    @Test
    void cannotUpgradeToTheSamePlanOrACheaperOne() throws Exception {
        User user = createMemberWithMembership("mismo.plan@test.local", completa, NOW);
        String token = loginAndGetToken("mismo.plan@test.local");

        String sameBody = objectMapper.writeValueAsString(new PlanChangeRequest(PlanCode.COMPLETA));
        mvc.perform(post("/api/v1/me/membership/upgrade").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(sameBody))
            .andExpect(status().isBadRequest());

        String cheaperBody = objectMapper.writeValueAsString(new PlanChangeRequest(PlanCode.BASICA));
        mvc.perform(post("/api/v1/me/membership/upgrade").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(cheaperBody))
            .andExpect(status().isBadRequest());

        Membership membership = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenant.getId(), user.getId()).orElseThrow();
        assertEquals(completa.getId(), membership.getPlanId());
    }

    @Test
    void schedulingADowngradeKeepsCurrentPlanUntilRenewalAndCanBeUndone() throws Exception {
        User user = createMemberWithMembership("baja.tarifa@test.local", atletaPro, NOW);
        String token = loginAndGetToken("baja.tarifa@test.local");

        String body = objectMapper.writeValueAsString(new PlanChangeRequest(PlanCode.BASICA));
        mvc.perform(post("/api/v1/me/membership/schedule-downgrade").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.planCode").value("ATLETA_PRO"))
            .andExpect(jsonPath("$.pendingPlanCode").value("BASICA"));

        Membership membership = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenant.getId(), user.getId()).orElseThrow();
        assertEquals(atletaPro.getId(), membership.getPlanId(), "el plan actual no cambia hasta la renovación");
        assertEquals(basica.getId(), membership.getPendingPlanId());

        mvc.perform(post("/api/v1/me/membership/cancel-downgrade").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pendingPlanCode").doesNotExist());

        membership = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenant.getId(), user.getId()).orElseThrow();
        assertEquals(null, membership.getPendingPlanId());
    }

    @Test
    void upgradingCancelsAPreviouslyScheduledDowngrade() throws Exception {
        User user = createMemberWithMembership("cambia.de.idea@test.local", completa, NOW);
        String token = loginAndGetToken("cambia.de.idea@test.local");

        mvc.perform(post("/api/v1/me/membership/schedule-downgrade").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PlanChangeRequest(PlanCode.BASICA))))
            .andExpect(status().isOk());

        mvc.perform(post("/api/v1/me/membership/upgrade").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PlanChangeRequest(PlanCode.ATLETA_PRO))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pendingPlanCode").doesNotExist());

        Membership membership = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenant.getId(), user.getId()).orElseThrow();
        assertEquals(null, membership.getPendingPlanId(), "subir cancela la bajada programada");
    }

    @Test
    void aMemberCanOnlyChangeTheirOwnMembership() throws Exception {
        User userA = createMemberWithMembership("socio.a@test.local", completa, NOW);
        createMemberWithMembership("socio.b@test.local", basica, NOW);
        String tokenA = loginAndGetToken("socio.a@test.local");

        mvc.perform(post("/api/v1/me/membership/upgrade").header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PlanChangeRequest(PlanCode.ATLETA_PRO))))
            .andExpect(status().isOk());

        User userB = users.findByEmailIgnoreCase("socio.b@test.local").orElseThrow();
        Membership membershipB = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenant.getId(), userB.getId()).orElseThrow();
        assertEquals(basica.getId(), membershipB.getPlanId(), "el cambio de A nunca debe tocar la membresía de B");

        Membership membershipA = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenant.getId(), userA.getId()).orElseThrow();
        assertEquals(atletaPro.getId(), membershipA.getPlanId());
    }

    @Test
    void membershipEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/me/membership/plan-options")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/me/membership/upgrade")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PlanChangeRequest(PlanCode.COMPLETA))))
            .andExpect(status().isForbidden());
    }

    @Test
    void dailyRenewalAppliesAScheduledDowngradeAndLogsIt() throws Exception {
        Instant periodStart = Instant.parse("2026-09-01T00:00:00Z");
        User user = createMemberWithMembership("renueva.con.bajada@test.local", completa, periodStart);
        Membership membership = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenant.getId(), user.getId()).orElseThrow();
        membership.scheduleDowngrade(basica.getId());
        memberships.save(membership);

        Mockito.when(clock.instant()).thenReturn(Instant.parse("2026-11-05T00:00:00Z"));
        renewalScheduler.renewExpiredMemberships();

        Membership renewed = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenant.getId(), user.getId()).orElseThrow();
        assertEquals(basica.getId(), renewed.getPlanId());
        assertEquals(null, renewed.getPendingPlanId());
        assertTrue(renewed.getPeriodEnd().isAfter(Instant.parse("2026-11-05T00:00:00Z")));

        List<MembershipPlanChange> audit = changes.findByMembershipIdOrderByCreatedAtDesc(membership.getId());
        assertTrue(audit.stream().anyMatch(c -> c.getChangeType() == PlanChangeType.DOWNGRADE_APPLIED));
        assertTrue(audit.stream().anyMatch(c -> c.getChangeType() == PlanChangeType.RENEWAL && c.getAmountCents() == basica.getPriceCents()));
        Mockito.verify(notifier, Mockito.atLeastOnce()).renewed(Mockito.any(), Mockito.any(), Mockito.anyInt());
    }

    @Test
    void normalRenewalWithoutAPendingPlanChargesTheFullCurrentPrice() throws Exception {
        Instant periodStart = Instant.parse("2026-09-01T00:00:00Z");
        User user = createMemberWithMembership("renueva.normal@test.local", basica, periodStart);
        Membership membership = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenant.getId(), user.getId()).orElseThrow();

        Mockito.when(clock.instant()).thenReturn(Instant.parse("2026-10-02T00:00:00Z"));
        renewalScheduler.renewExpiredMemberships();

        Membership renewed = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenant.getId(), user.getId()).orElseThrow();
        assertEquals(basica.getId(), renewed.getPlanId());

        List<MembershipPlanChange> audit = changes.findByMembershipIdOrderByCreatedAtDesc(membership.getId());
        assertEquals(1, audit.size());
        assertEquals(PlanChangeType.RENEWAL, audit.get(0).getChangeType());
        assertEquals(basica.getPriceCents(), audit.get(0).getAmountCents());
        assertEquals(null, audit.get(0).getChangedByUserId(), "lo hizo el job, no una persona");
    }

    @Test
    void renewalNoticeFiresOnlyOncePerPeriod() throws Exception {
        Instant periodStart = Instant.parse("2026-10-01T00:00:00Z");
        User user = createMemberWithMembership("aviso.renovacion@test.local", completa, periodStart);

        Mockito.when(clock.instant()).thenReturn(Instant.parse("2026-10-29T00:00:00Z"));
        renewalScheduler.sendUpcomingRenewalNotices();
        renewalScheduler.sendUpcomingRenewalNotices();

        // Filtra por este socio: otras pruebas de esta misma clase dejan en la BD membresías
        // que podrían caer también dentro de la ventana de aviso de esta prueba en concreto.
        Mockito.verify(notifier, Mockito.times(1))
            .renewalUpcoming(Mockito.argThat(m -> m.getUserId().equals(user.getId())), Mockito.any(), Mockito.any());
    }
}
