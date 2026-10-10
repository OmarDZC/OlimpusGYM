package com.gymflow.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.assistant.ai.AssistantClient;
import com.gymflow.assistant.ai.AssistantFailedException;
import com.gymflow.assistant.ai.AssistantUnavailableException;
import com.gymflow.assistant.dto.AssistantChatRequest;
import com.gymflow.assistant.dto.AssistantChatResponse;
import com.gymflow.assistant.dto.ChatMessageDto;
import com.gymflow.assistant.dto.PendingActionDto;
import com.gymflow.auth.Role;
import com.gymflow.auth.User;
import com.gymflow.auth.UserRepository;
import com.gymflow.auth.dto.LoginRequest;
import com.gymflow.plan.Membership;
import com.gymflow.plan.MembershipRepository;
import com.gymflow.plan.MembershipStatus;
import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanCode;
import com.gymflow.plan.PlanRepository;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
 * Trozo 6a: el gate por tarifa (`ai_assistant`) y la propagación de errores del servicio de
 * IA viven en el backend; el tool calling y las defensas de prompt injection se prueban en
 * `ai-service` con un LLM simulado (`tests/test_assistant.py`). `AssistantClient` se sustituye
 * por un doble (`@MockitoBean`): este test no habla con ningún servicio de IA real.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class AssistantHttpTest {

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

    @MockitoBean
    AssistantClient assistantClient;

    private Tenant tenant;
    private Plan planWithAssistant;
    private Plan planWithoutAssistant;

    @BeforeEach
    void setUp() {
        tenant = tenants.findBySlug(Tenant.DEFAULT_SLUG)
            .orElseGet(() -> tenants.save(new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG)));
        planWithAssistant = plans.findByTenantIdAndCode(tenant.getId(), PlanCode.COMPLETA)
            .orElseGet(() -> plans.save(new Plan(tenant.getId(), PlanCode.COMPLETA, "Completa", 5900, null, 14, true, true)));
        planWithoutAssistant = plans.findByTenantIdAndCode(tenant.getId(), PlanCode.BASICA)
            .orElseGet(() -> plans.save(new Plan(tenant.getId(), PlanCode.BASICA, "Básica", 3900, 4, 7, false, false)));
        Mockito.reset(assistantClient);
    }

    private User createMemberWithMembership(String email, Plan plan) {
        User user = new User(tenant.getId(), email, passwordEncoder.encode("ClaveDePrueba123!"), "Socio Prueba", Role.MEMBER);
        users.save(user);
        memberships.save(new Membership(tenant.getId(), user.getId(), plan.getId(), MembershipStatus.ACTIVE, Instant.now()));
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

    private String chatBody(String message) throws Exception {
        return objectMapper.writeValueAsString(Map.of("messages", List.of(new ChatMessageDto("user", message))));
    }

    @Test
    void chatProxiesToTheAiServiceWhenTariffIncludesAssistant() throws Exception {
        createMemberWithMembership("con.asistente@test.local", planWithAssistant);
        String token = loginAndGetToken("con.asistente@test.local");
        Mockito.when(assistantClient.chat(Mockito.eq(token), Mockito.any()))
            .thenReturn(new AssistantChatResponse("Hay tres clases de Yoga esta semana.", null, null));

        mvc.perform(post("/api/v1/me/assistant/chat").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(chatBody("¿Qué clases de yoga hay?")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reply").value("Hay tres clases de Yoga esta semana."));
    }

    @Test
    void chatIsForbiddenWhenTariffHasNoAssistant_andNeverCallsTheAiService() throws Exception {
        createMemberWithMembership("sin.asistente@test.local", planWithoutAssistant);
        String token = loginAndGetToken("sin.asistente@test.local");

        mvc.perform(post("/api/v1/me/assistant/chat").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(chatBody("Resérvame una clase")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Tu tarifa no incluye el asistente con IA."));

        Mockito.verifyNoInteractions(assistantClient);
    }

    @Test
    void chatReturns503WhenAiServiceIsUnreachable() throws Exception {
        createMemberWithMembership("ia.caida@test.local", planWithAssistant);
        String token = loginAndGetToken("ia.caida@test.local");
        Mockito.when(assistantClient.chat(Mockito.eq(token), Mockito.any())).thenThrow(new AssistantUnavailableException());

        mvc.perform(post("/api/v1/me/assistant/chat").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(chatBody("hola")))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void chatReturns502WhenAiServiceFailsToResolveTheConversation() throws Exception {
        createMemberWithMembership("ia.falla@test.local", planWithAssistant);
        String token = loginAndGetToken("ia.falla@test.local");
        Mockito.when(assistantClient.chat(Mockito.eq(token), Mockito.any())).thenThrow(new AssistantFailedException());

        mvc.perform(post("/api/v1/me/assistant/chat").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(chatBody("hola")))
            .andExpect(status().isBadGateway());
    }

    @Test
    void chatRejectsAMessageThatIsTooLong_andNeverCallsTheAiService() throws Exception {
        createMemberWithMembership("mensaje.largo@test.local", planWithAssistant);
        String token = loginAndGetToken("mensaje.largo@test.local");

        mvc.perform(post("/api/v1/me/assistant/chat").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(chatBody("a".repeat(2001))))
            .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(assistantClient);
    }

    @Test
    void chatForwardsTheConfirmActionUntouchedToTheAiService() throws Exception {
        createMemberWithMembership("confirma.accion@test.local", planWithAssistant);
        String token = loginAndGetToken("confirma.accion@test.local");
        Mockito.when(assistantClient.chat(Mockito.eq(token), Mockito.any()))
            .thenReturn(new AssistantChatResponse("¡Reserva confirmada!", null, null));

        var confirm = new PendingActionDto("book", java.util.UUID.randomUUID(), null, "Reservar Yoga el martes", null);
        String body = objectMapper.writeValueAsString(Map.of("messages", List.of(), "confirmAction", confirm));

        mvc.perform(post("/api/v1/me/assistant/chat").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reply").value("¡Reserva confirmada!"))
            .andExpect(jsonPath("$.pendingAction").doesNotExist());

        ArgumentCaptor<AssistantChatRequest> captor = ArgumentCaptor.forClass(AssistantChatRequest.class);
        Mockito.verify(assistantClient).chat(Mockito.eq(token), captor.capture());
        assertThat(captor.getValue().confirmAction().type()).isEqualTo("book");
        assertThat(captor.getValue().confirmAction().sessionId()).isEqualTo(confirm.sessionId());
    }

    @Test
    void chatEndpointRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/me/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON).content(chatBody("hola")))
            .andExpect(status().isForbidden());
    }
}
