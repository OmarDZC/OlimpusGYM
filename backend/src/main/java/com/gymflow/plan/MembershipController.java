package com.gymflow.plan;

import com.gymflow.common.TenantContext;
import com.gymflow.plan.dto.MembershipResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/membership")
public class MembershipController {

    private final MembershipRepository memberships;
    private final PlanRepository plans;

    public MembershipController(MembershipRepository memberships, PlanRepository plans) {
        this.memberships = memberships;
        this.plans = plans;
    }

    @GetMapping
    public MembershipResponse membership(Authentication authentication) {
        UUID tenantId = TenantContext.require();
        UUID userId = (UUID) authentication.getPrincipal();

        Membership membership = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenantId, userId)
                .orElseThrow(MembershipNotFoundException::new);
        Plan plan = plans.findById(membership.getPlanId())
                .orElseThrow(() -> new IllegalStateException("La tarifa de la membresía ya no existe"));

        return new MembershipResponse(
                plan.getCode().name(),
                plan.getName(),
                plan.getPriceCents(),
                membership.getStatus().name(),
                membership.getStartedAt(),
                membership.getRenewsAt(),
                plan.isAiTrainer(),
                plan.isAiAssistant());
    }
}
