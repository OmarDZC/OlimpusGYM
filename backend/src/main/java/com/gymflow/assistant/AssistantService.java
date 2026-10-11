package com.gymflow.assistant;

import com.gymflow.ai.AiFeature;
import com.gymflow.ai.AiUsageLimiter;
import com.gymflow.assistant.ai.AssistantClient;
import com.gymflow.assistant.dto.AssistantChatRequest;
import com.gymflow.assistant.dto.AssistantChatResponse;
import com.gymflow.plan.Membership;
import com.gymflow.plan.MembershipRepository;
import com.gymflow.plan.MembershipStatus;
import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Asistente de reservas con IA (LOGIC.md §6 y §7): proxy al servicio de IA con el token del socio. */
@Service
public class AssistantService {

    private final MembershipRepository memberships;
    private final PlanRepository plans;
    private final AssistantClient assistantClient;
    private final AiUsageLimiter aiUsageLimiter;

    public AssistantService(
            MembershipRepository memberships,
            PlanRepository plans,
            AssistantClient assistantClient,
            AiUsageLimiter aiUsageLimiter) {
        this.memberships = memberships;
        this.plans = plans;
        this.assistantClient = assistantClient;
        this.aiUsageLimiter = aiUsageLimiter;
    }

    public AssistantChatResponse chat(UUID tenantId, UUID userId, String memberToken, AssistantChatRequest request) {
        Membership membership = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenantId, userId)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> new AssistantNotAllowedException("No tienes una cuota activa."));
        Plan plan = plans.findById(membership.getPlanId())
                .orElseThrow(() -> new IllegalStateException("La tarifa de la membresía ya no existe"));
        if (!plan.isAiAssistant()) {
            throw new AssistantNotAllowedException("Tu tarifa no incluye el asistente con IA.");
        }
        aiUsageLimiter.checkAndRecordUsage(tenantId, userId, plan.getCode(), AiFeature.ASSISTANT_MESSAGE);

        return assistantClient.chat(memberToken, request);
    }
}
