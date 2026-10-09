package com.gymflow.plan;

import com.gymflow.common.TenantContext;
import com.gymflow.plan.dto.MembershipResponse;
import com.gymflow.plan.dto.PlanChangeOptionDto;
import com.gymflow.plan.dto.PlanChangeRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cuota del socio y cambio de tarifa (Trozo 9b, LOGIC.md §12). Un socio aquí siempre actúa
 * sobre su propia membresía: el identificador nunca viene del cliente, sale del token.
 */
@RestController
@RequestMapping("/api/v1/me/membership")
public class MembershipController {

    private final MembershipPlanService membershipPlanService;

    public MembershipController(MembershipPlanService membershipPlanService) {
        this.membershipPlanService = membershipPlanService;
    }

    @GetMapping
    public MembershipResponse membership(Authentication authentication) {
        return membershipPlanService.getMembership(TenantContext.require(), userId(authentication));
    }

    @GetMapping("/plan-options")
    public List<PlanChangeOptionDto> planOptions(Authentication authentication) {
        return membershipPlanService.listOptions(TenantContext.require(), userId(authentication));
    }

    @PostMapping("/upgrade")
    public MembershipResponse upgrade(Authentication authentication, @Valid @RequestBody PlanChangeRequest request) {
        UUID userId = userId(authentication);
        return membershipPlanService.upgradeNow(TenantContext.require(), userId, userId, request.planCode());
    }

    @PostMapping("/schedule-downgrade")
    public MembershipResponse scheduleDowngrade(Authentication authentication, @Valid @RequestBody PlanChangeRequest request) {
        UUID userId = userId(authentication);
        return membershipPlanService.scheduleDowngrade(TenantContext.require(), userId, userId, request.planCode());
    }

    @PostMapping("/cancel-downgrade")
    public MembershipResponse cancelDowngrade(Authentication authentication) {
        UUID userId = userId(authentication);
        return membershipPlanService.cancelScheduledDowngrade(TenantContext.require(), userId, userId);
    }

    private UUID userId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
