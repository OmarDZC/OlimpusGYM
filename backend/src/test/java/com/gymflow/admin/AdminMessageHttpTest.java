package com.gymflow.admin;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.auth.Role;
import com.gymflow.auth.User;
import com.gymflow.auth.UserRepository;
import com.gymflow.auth.dto.LoginRequest;
import com.gymflow.contact.ContactMessage;
import com.gymflow.contact.ContactMessageRepository;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.time.Instant;
import java.util.UUID;
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
 * Mensajes de contacto en el panel de gestión (Trozo 7b, LOGIC.md §6): OWNER y STAFF por
 * igual, un socio recibe 403. Un tenant nuevo por test evita que los mensajes de uno se
 * cuelen en el listado de otro. Se omite automáticamente si no hay Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class AdminMessageHttpTest {

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
    ContactMessageRepository messages;

    @Autowired
    PasswordEncoder passwordEncoder;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString();
        tenant = tenants.save(new Tenant("Gimnasio " + unique, "gimnasio-" + unique));
    }

    private User createUser(String email, Role role) {
        User user = new User(tenant.getId(), email, passwordEncoder.encode("ClaveDePrueba123!"), "Usuario Prueba", role);
        return users.save(user);
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

    private ContactMessage createMessage(String name, Instant createdAt) {
        return messages.save(new ContactMessage(tenant.getId(), name, "socia@test.local", "Hola", createdAt));
    }

    @Test
    void ownerSeesMessagesMostRecentFirst() throws Exception {
        createUser("owner@test.local", Role.OWNER);
        createMessage("Primero", Instant.parse("2026-10-09T10:00:00Z"));
        createMessage("Segundo", Instant.parse("2026-10-09T12:00:00Z"));
        String token = loginAndGetToken("owner@test.local");

        mvc.perform(get("/api/v1/admin/messages").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Segundo"))
                .andExpect(jsonPath("$[0].read").value(false))
                .andExpect(jsonPath("$[1].name").value("Primero"));
    }

    @Test
    void staffCanAlsoSeeMessages() throws Exception {
        createUser("staff@test.local", Role.STAFF);
        createMessage("Mensaje", Instant.parse("2026-10-09T10:00:00Z"));
        String token = loginAndGetToken("staff@test.local");

        mvc.perform(get("/api/v1/admin/messages").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void memberGets403() throws Exception {
        createUser("socio@test.local", Role.MEMBER);
        String token = loginAndGetToken("socio@test.local");

        mvc.perform(get("/api/v1/admin/messages").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void markingAMessageAsReadPersistsIt() throws Exception {
        createUser("owner.marca@test.local", Role.OWNER);
        ContactMessage message = createMessage("Mensaje", Instant.parse("2026-10-09T10:00:00Z"));
        String token = loginAndGetToken("owner.marca@test.local");

        mvc.perform(patch("/api/v1/admin/messages/" + message.getId() + "/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        ContactMessage reloaded = messages.findById(message.getId()).orElseThrow();
        assertNotNull(reloaded.getReadAt());
    }

    @Test
    void messagesEndpointRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/admin/messages")).andExpect(status().isForbidden());
    }
}
