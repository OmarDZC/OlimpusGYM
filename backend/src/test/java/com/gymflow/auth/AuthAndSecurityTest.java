package com.gymflow.auth;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.auth.dto.LoginRequest;
import com.gymflow.auth.dto.RegisterRequest;
import com.gymflow.plan.Membership;
import com.gymflow.plan.MembershipRepository;
import com.gymflow.plan.MembershipStatus;
import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanCode;
import com.gymflow.plan.PlanRepository;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Cubre LOGIC.md §2 (registro, login, /me, permisos por rol) y el aislamiento
 * por tenant. Se omite automáticamente si no hay Docker disponible.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class AuthAndSecurityTest {

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
    PasswordEncoder passwordEncoder;

    private Tenant tenant;

    @BeforeEach
    void ensureTenantAndPlans() {
        tenant = tenants.findBySlug(Tenant.DEFAULT_SLUG)
            .orElseGet(() -> tenants.save(new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG)));
        ensurePlan(tenant, PlanCode.BASICA, 3900, 4, 7, false, false);
        ensurePlan(tenant, PlanCode.COMPLETA, 5900, null, 14, true, true);
        ensurePlan(tenant, PlanCode.ATLETA_PRO, 7900, null, 14, true, true);
    }

    private void ensurePlan(
            Tenant t, PlanCode code, int price, Integer classesPerMonth, int window, boolean trainer, boolean assistant) {
        plans.findByTenantIdAndCode(t.getId(), code)
            .orElseGet(() -> plans.save(new Plan(t.getId(), code, code.name(), price, classesPerMonth, window, trainer, assistant)));
    }

    private String registerAndGetToken(String email, String password, String fullName, PlanCode planCode) throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterRequest(email, password, fullName, planCode));
        String response = mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private User insertUserDirectly(Tenant t, String email, String fullName, Role role) {
        User user = new User(t.getId(), email, passwordEncoder.encode("ClaveDirecta123!"), fullName, role);
        return users.save(user);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(email, password));
        String response = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void registerLoginAndMeHappyPath() throws Exception {
        String email = "ana.registro@olympusgym.test";
        String token = registerAndGetToken(email, "Contrasena123!", "Ana Registro", PlanCode.COMPLETA);

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.role").value("MEMBER"))
            .andExpect(jsonPath("$.tenantId").value(tenant.getId().toString()));

        String loginToken = loginAndGetToken(email, "Contrasena123!");
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + loginToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void registerWithDuplicateEmailReturns409() throws Exception {
        String email = "duplicado@olympusgym.test";
        registerAndGetToken(email, "Contrasena123!", "Primera Vez", PlanCode.BASICA);

        String body = objectMapper.writeValueAsString(
            new RegisterRequest(email, "OtraContrasena123!", "Segunda Vez", PlanCode.BASICA));
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        String email = "password.incorrecta@olympusgym.test";
        registerAndGetToken(email, "Contrasena123!", "Alguien", PlanCode.BASICA);

        String body = objectMapper.writeValueAsString(new LoginRequest(email, "NoEsEsta123!"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void memberCannotAccessAdminEndpoints() throws Exception {
        String token = registerAndGetToken("socio.sin.permiso@olympusgym.test", "Contrasena123!", "Socio", PlanCode.BASICA);

        mvc.perform(get("/api/v1/admin/ping").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void staffCanAccessAdminPingButNotOwnerOnly() throws Exception {
        insertUserDirectly(tenant, "staff.prueba@olympusgym.test", "Staff Prueba", Role.STAFF);
        String token = loginAndGetToken("staff.prueba@olympusgym.test", "ClaveDirecta123!");

        mvc.perform(get("/api/v1/admin/ping").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        mvc.perform(get("/api/v1/admin/owner/ping").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanAccessOwnerOnlyEndpoint() throws Exception {
        insertUserDirectly(tenant, "owner.prueba@olympusgym.test", "Owner Prueba", Role.OWNER);
        String token = loginAndGetToken("owner.prueba@olympusgym.test", "ClaveDirecta123!");

        mvc.perform(get("/api/v1/admin/owner/ping").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void tenantsStayIsolated() throws Exception {
        Tenant otherTenant = tenants.findBySlug("otro-gimnasio")
            .orElseGet(() -> tenants.save(new Tenant("Otro Gimnasio", "otro-gimnasio")));
        assertNotEquals(tenant.getId(), otherTenant.getId());

        ensurePlan(otherTenant, PlanCode.BASICA, 3000, 4, 7, false, false);
        Plan otherPlan = plans.findByTenantIdAndCode(otherTenant.getId(), PlanCode.BASICA).orElseThrow();

        User otherTenantUser = insertUserDirectly(otherTenant, "socio@otrogimnasio.test", "Socio Ajeno", Role.MEMBER);
        memberships.save(new Membership(
            otherTenant.getId(), otherTenantUser.getId(), otherPlan.getId(), MembershipStatus.ACTIVE, Instant.now()));

        String token = loginAndGetToken("socio@otrogimnasio.test", "ClaveDirecta123!");

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantId").value(otherTenant.getId().toString()));
    }
}
