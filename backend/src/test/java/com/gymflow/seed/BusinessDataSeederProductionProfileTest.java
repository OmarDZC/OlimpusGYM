package com.gymflow.seed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gymflow.auth.UserRepository;
import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanCode;
import com.gymflow.plan.PlanRepository;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Trozo 8a-2: con el perfil "prod" activo, el tenant único y las 3 tarifas reales de
 * LOGIC.md §3 existen de verdad (los crea {@link BusinessDataSeeder}, que corre en todos los
 * perfiles), pero no hay ni un socio ni una cuenta de demo (el seed de ejemplo,
 * {@link DevDataSeeder}, está limitado a dev/demo y aquí no corre). Solo existe la cuenta real
 * del administrador que crea {@link ProductionAdminSeeder}.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("prod")
class BusinessDataSeederProductionProfileTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("gymflow.jwt.secret", () -> "test-only-secret-not-for-real-use-1234567890");
        registry.add("gymflow.admin.email", () -> "admin-test@example.com");
        registry.add("gymflow.admin.password", () -> "contraseña-de-prueba-123");
    }

    @Autowired
    TenantRepository tenants;

    @Autowired
    PlanRepository plans;

    @Autowired
    UserRepository users;

    @Test
    void theTenantAndTheThreeRealPlansExist() {
        Tenant tenant = tenants.findBySlug(Tenant.DEFAULT_SLUG).orElseThrow();

        List<PlanCode> codes = plans.findByTenantIdAndActiveTrue(tenant.getId()).stream().map(Plan::getCode).toList();
        assertEquals(3, codes.size());
        assertTrue(codes.containsAll(List.of(PlanCode.BASICA, PlanCode.COMPLETA, PlanCode.ATLETA_PRO)));

        Plan basica = plans.findByTenantIdAndCode(tenant.getId(), PlanCode.BASICA).orElseThrow();
        assertEquals(3900, basica.getPriceCents());
        assertEquals(4, basica.getClassesPerMonth());
        assertFalse(basica.isAiTrainer());

        Plan completa = plans.findByTenantIdAndCode(tenant.getId(), PlanCode.COMPLETA).orElseThrow();
        assertEquals(5900, completa.getPriceCents());
        assertEquals(null, completa.getClassesPerMonth());
        assertTrue(completa.isAiTrainer());
        assertTrue(completa.isAiAssistant());

        Plan pro = plans.findByTenantIdAndCode(tenant.getId(), PlanCode.ATLETA_PRO).orElseThrow();
        assertEquals(7900, pro.getPriceCents());
    }

    @Test
    void thereAreNoMembersNorDemoAccountsOnlyTheRealAdmin() {
        assertEquals(1, users.count());
        assertTrue(users.findByEmailIgnoreCase("javier@olympusgym.test").isEmpty());
        assertTrue(users.findByEmailIgnoreCase("omaradmin@gmail.com").isEmpty());
        assertTrue(users.findByEmailIgnoreCase("carlos@olympusgym.test").isEmpty());
        assertTrue(users.findByEmailIgnoreCase("admin-test@example.com").isPresent());
    }
}
