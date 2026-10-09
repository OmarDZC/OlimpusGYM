package com.gymflow.plan;

import com.gymflow.plan.dto.PublicPlanDto;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tarifas para la web pública (Trozo 7a, LOGIC.md §6): sin sesión, para que `/tarifas` y `/` no
 * dupliquen a mano los precios y beneficios que ya vive el backend (igual que Mi cuota).
 */
@RestController
@RequestMapping("/api/v1/public/plans")
public class PublicPlanController {

    private final PlanRepository plans;
    private final TenantRepository tenants;

    public PublicPlanController(PlanRepository plans, TenantRepository tenants) {
        this.plans = plans;
        this.tenants = tenants;
    }

    @GetMapping
    public List<PublicPlanDto> list() {
        UUID tenantId = tenants.findBySlug(Tenant.DEFAULT_SLUG)
                .orElseThrow(() -> new IllegalStateException("No existe el tenant por defecto"))
                .getId();

        return plans.findByTenantIdAndActiveTrue(tenantId).stream()
                .sorted(Comparator.comparingInt(Plan::getPriceCents))
                .map(plan -> new PublicPlanDto(
                        plan.getCode().name(),
                        plan.getName(),
                        plan.getPriceCents(),
                        plan.getClassesPerMonth(),
                        plan.isAiTrainer(),
                        plan.isAiAssistant()))
                .toList();
    }
}
