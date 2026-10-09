package com.gymflow.plan;

import com.gymflow.plan.dto.MembershipResponse;
import com.gymflow.plan.dto.PlanChangeOptionDto;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Subir/bajar de tarifa, con periodos coherentes (Trozo 9b, LOGIC.md §12). Subir es siempre al
 * instante y se cobra la diferencia prorrateada; bajar siempre se programa para la siguiente
 * renovación (la aplica {@link MembershipRenewalScheduler}). Lo usan tanto el autoservicio del
 * propio socio como el cambio de tarifa del dueño desde el panel, para que las reglas nunca
 * diverjan según quién lo pida.
 */
@Service
public class MembershipPlanService {

    private final MembershipRepository memberships;
    private final PlanRepository plans;
    private final MembershipPlanChangeRepository changes;
    private final Clock clock;

    public MembershipPlanService(
            MembershipRepository memberships,
            PlanRepository plans,
            MembershipPlanChangeRepository changes,
            Clock clock) {
        this.memberships = memberships;
        this.plans = plans;
        this.changes = changes;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MembershipResponse getMembership(UUID tenantId, UUID userId) {
        Membership membership = requireMembership(tenantId, userId);
        Plan plan = requirePlan(tenantId, membership.getPlanId());
        return toResponse(membership, plan);
    }

    @Transactional(readOnly = true)
    public List<PlanChangeOptionDto> listOptions(UUID tenantId, UUID targetUserId) {
        Membership membership = requireMembership(tenantId, targetUserId);
        Plan currentPlan = requirePlan(tenantId, membership.getPlanId());

        return plans.findByTenantIdAndActiveTrue(tenantId).stream()
                .sorted(Comparator.comparingInt(Plan::getPriceCents))
                .map(plan -> toOption(plan, membership, currentPlan))
                .toList();
    }

    @Transactional
    public MembershipResponse upgradeNow(UUID tenantId, UUID actingUserId, UUID targetUserId, PlanCode newPlanCode) {
        Membership membership = requireMembership(tenantId, targetUserId);
        Plan currentPlan = requirePlan(tenantId, membership.getPlanId());
        Plan newPlan = requirePlanByCode(tenantId, newPlanCode);

        if (newPlan.getPriceCents() <= currentPlan.getPriceCents()) {
            throw new IllegalArgumentException(
                    "Solo puedes \"subir ahora\" a una tarifa más cara que la actual. Para una tarifa más "
                            + "barata, se programa para la siguiente renovación.");
        }

        int amountCents = ProrationCalculator.upgradeAmountCents(
                currentPlan.getPriceCents(), newPlan.getPriceCents(), clock.instant(),
                membership.getPeriodStart(), membership.getPeriodEnd());

        membership.applyUpgrade(newPlan.getId());
        memberships.save(membership);
        recordChange(membership, actingUserId, currentPlan, newPlan, PlanChangeType.UPGRADE, amountCents);

        return toResponse(membership, newPlan);
    }

    @Transactional
    public MembershipResponse scheduleDowngrade(UUID tenantId, UUID actingUserId, UUID targetUserId, PlanCode newPlanCode) {
        Membership membership = requireMembership(tenantId, targetUserId);
        Plan currentPlan = requirePlan(tenantId, membership.getPlanId());
        Plan newPlan = requirePlanByCode(tenantId, newPlanCode);

        if (newPlan.getPriceCents() >= currentPlan.getPriceCents()) {
            throw new IllegalArgumentException("Esa tarifa no es más barata que la actual: para subir, usa \"subir ahora\".");
        }

        membership.scheduleDowngrade(newPlan.getId());
        memberships.save(membership);
        recordChange(membership, actingUserId, currentPlan, newPlan, PlanChangeType.DOWNGRADE_SCHEDULED, null);

        return toResponse(membership, currentPlan);
    }

    @Transactional
    public MembershipResponse cancelScheduledDowngrade(UUID tenantId, UUID actingUserId, UUID targetUserId) {
        Membership membership = requireMembership(tenantId, targetUserId);
        if (membership.getPendingPlanId() == null) {
            throw new IllegalArgumentException("No tienes ninguna bajada de tarifa programada.");
        }
        Plan currentPlan = requirePlan(tenantId, membership.getPlanId());
        Plan pendingPlan = requirePlan(tenantId, membership.getPendingPlanId());

        membership.cancelScheduledDowngrade();
        memberships.save(membership);
        recordChange(membership, actingUserId, currentPlan, pendingPlan, PlanChangeType.DOWNGRADE_CANCELLED, null);

        return toResponse(membership, currentPlan);
    }

    /**
     * Cambio desde el panel de gestión (solo OWNER, LOGIC.md §6): un único selector, sin pasos
     * de "subir ahora"/"programar bajada" por separado, así que aquí se decide solo según el
     * precio. Reutiliza las mismas reglas que el autoservicio para que los periodos nunca se
     * desincronicen de los de un socio que cambia su propia tarifa.
     */
    @Transactional
    public MembershipResponse changePlanAsOwner(UUID tenantId, UUID ownerUserId, UUID memberId, PlanCode newPlanCode) {
        Membership membership = requireMembership(tenantId, memberId);
        Plan currentPlan = requirePlan(tenantId, membership.getPlanId());
        Plan newPlan = requirePlanByCode(tenantId, newPlanCode);

        if (newPlan.getId().equals(currentPlan.getId())) {
            throw new IllegalArgumentException("Ese socio ya tiene esa tarifa.");
        }
        if (newPlan.getPriceCents() > currentPlan.getPriceCents()) {
            return upgradeNow(tenantId, ownerUserId, memberId, newPlanCode);
        }
        return scheduleDowngrade(tenantId, ownerUserId, memberId, newPlanCode);
    }

    private PlanChangeOptionDto toOption(Plan plan, Membership membership, Plan currentPlan) {
        if (plan.getId().equals(currentPlan.getId())) {
            return new PlanChangeOptionDto(plan.getCode().name(), plan.getName(), plan.getPriceCents(), "CURRENT", null, null);
        }
        if (plan.getPriceCents() > currentPlan.getPriceCents()) {
            int amount = ProrationCalculator.upgradeAmountCents(
                    currentPlan.getPriceCents(), plan.getPriceCents(), clock.instant(),
                    membership.getPeriodStart(), membership.getPeriodEnd());
            return new PlanChangeOptionDto(plan.getCode().name(), plan.getName(), plan.getPriceCents(), "UPGRADE", amount, null);
        }
        return new PlanChangeOptionDto(
                plan.getCode().name(), plan.getName(), plan.getPriceCents(), "DOWNGRADE", null, membership.getPeriodEnd());
    }

    private void recordChange(
            Membership membership, UUID actingUserId, Plan fromPlan, Plan toPlan, PlanChangeType type, Integer amountCents) {
        changes.save(new MembershipPlanChange(
                membership.getTenantId(), membership.getId(), actingUserId,
                fromPlan.getId(), toPlan.getId(), type, amountCents, clock.instant()));
    }

    private MembershipResponse toResponse(Membership membership, Plan plan) {
        Plan pendingPlan = membership.getPendingPlanId() != null
                ? plans.findById(membership.getPendingPlanId()).orElse(null)
                : null;
        return new MembershipResponse(
                plan.getCode().name(),
                plan.getName(),
                plan.getPriceCents(),
                plan.getClassesPerMonth(),
                membership.getStatus().name(),
                membership.getStartedAt(),
                membership.getPeriodStart(),
                membership.getPeriodEnd(),
                plan.isAiTrainer(),
                plan.isAiAssistant(),
                pendingPlan != null ? pendingPlan.getCode().name() : null,
                pendingPlan != null ? pendingPlan.getName() : null);
    }

    private Membership requireMembership(UUID tenantId, UUID userId) {
        return memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenantId, userId)
                .orElseThrow(MembershipNotFoundException::new);
    }

    private Plan requirePlan(UUID tenantId, UUID planId) {
        return plans.findById(planId)
                .filter(p -> p.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalStateException("La tarifa de la membresía ya no existe"));
    }

    private Plan requirePlanByCode(UUID tenantId, PlanCode code) {
        return plans.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new IllegalArgumentException("Tarifa no disponible: " + code));
    }
}
