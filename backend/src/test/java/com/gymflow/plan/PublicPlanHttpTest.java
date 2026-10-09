package com.gymflow.plan;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Tarifas para la web pública (Trozo 7a, LOGIC.md §6): sin sesión, sin precio del socio, para
 * que `/tarifas` no duplique a mano lo que ya vive en el backend. Se omite sin Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class PublicPlanHttpTest {

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
    TenantRepository tenants;

    @Autowired
    PlanRepository plans;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenants.findBySlug(Tenant.DEFAULT_SLUG)
                .orElseGet(() -> tenants.save(new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG)));
        plans.findByTenantIdAndCode(tenant.getId(), PlanCode.BASICA)
                .orElseGet(() -> plans.save(new Plan(tenant.getId(), PlanCode.BASICA, "Básica", 3900, 4, 7, false, false)));
        plans.findByTenantIdAndCode(tenant.getId(), PlanCode.COMPLETA)
                .orElseGet(() -> plans.save(new Plan(tenant.getId(), PlanCode.COMPLETA, "Completa", 5900, null, 14, true, true)));
        plans.findByTenantIdAndCode(tenant.getId(), PlanCode.ATLETA_PRO)
                .orElseGet(() -> plans.save(new Plan(tenant.getId(), PlanCode.ATLETA_PRO, "Atleta Pro", 7900, null, 14, true, true)));
    }

    @Test
    void listsTheThreePlansOrderedByPriceWithoutAuthentication() throws Exception {
        mvc.perform(get("/api/v1/public/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].planCode").value("BASICA"))
                .andExpect(jsonPath("$[0].priceCents").value(3900))
                .andExpect(jsonPath("$[0].aiTrainer").value(false))
                .andExpect(jsonPath("$[1].planCode").value("COMPLETA"))
                .andExpect(jsonPath("$[2].planCode").value("ATLETA_PRO"))
                .andExpect(jsonPath("$[2].aiAssistant").value(true));
    }
}
