package com.gymflow.plan;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job diario de renovación y aviso de cuota (Trozo 9b, LOGIC.md §12). Simula el cobro: abre el
 * siguiente periodo, aplica la bajada de tarifa programada si había una, y registra el
 * movimiento. Usa el {@link Clock} inyectado para que los tests puedan fijar "ahora".
 */
@Component
public class MembershipRenewalScheduler {

    private static final Duration RENEWAL_NOTICE_WINDOW = Duration.ofDays(3);

    private final MembershipRepository memberships;
    private final PlanRepository plans;
    private final MembershipPlanChangeRepository changes;
    private final MembershipNotifier notifier;
    private final Clock clock;

    public MembershipRenewalScheduler(
            MembershipRepository memberships,
            PlanRepository plans,
            MembershipPlanChangeRepository changes,
            MembershipNotifier notifier,
            Clock clock) {
        this.memberships = memberships;
        this.plans = plans;
        this.changes = changes;
        this.notifier = notifier;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    public void run() {
        renewExpiredMemberships();
        sendUpcomingRenewalNotices();
    }

    /**
     * Si el job lleva días sin ejecutarse, una membresía puede tener varios periodos vencidos:
     * se renueva en bucle hasta que el periodo abierto cubra el "ahora" actual.
     */
    @Transactional
    void renewExpiredMemberships() {
        Instant now = clock.instant();
        for (Membership membership : memberships.findByStatusAndPeriodEndLessThanEqual(MembershipStatus.ACTIVE, now)) {
            while (!membership.getPeriodEnd().isAfter(now)) {
                Plan planBeforeRenewal = requirePlan(membership.getPlanId());
                Optional<UUID> appliedDowngrade = membership.renew(now);
                Plan newPlan = requirePlan(membership.getPlanId());

                if (appliedDowngrade.isPresent()) {
                    recordChange(membership, planBeforeRenewal, newPlan, PlanChangeType.DOWNGRADE_APPLIED, null, now);
                }
                recordChange(membership, newPlan, newPlan, PlanChangeType.RENEWAL, newPlan.getPriceCents(), now);
                notifier.renewed(membership, newPlan, newPlan.getPriceCents());
            }
            memberships.save(membership);
        }
    }

    @Transactional
    void sendUpcomingRenewalNotices() {
        Instant now = clock.instant();
        Instant horizon = now.plus(RENEWAL_NOTICE_WINDOW);
        for (Membership membership : memberships.findByStatusAndPeriodEndBetweenAndRenewalNoticeSentAtIsNull(
                MembershipStatus.ACTIVE, now, horizon)) {
            Plan currentPlan = requirePlan(membership.getPlanId());
            notifier.renewalUpcoming(membership, currentPlan, membership.getPeriodEnd());
            membership.markRenewalNoticeSent(now);
            memberships.save(membership);
        }
    }

    private Plan requirePlan(UUID planId) {
        return plans.findById(planId)
                .orElseThrow(() -> new IllegalStateException("La tarifa de la membresía ya no existe"));
    }

    private void recordChange(
            Membership membership, Plan fromPlan, Plan toPlan, PlanChangeType type, Integer amountCents, Instant now) {
        changes.save(new MembershipPlanChange(
                membership.getTenantId(), membership.getId(), null, fromPlan.getId(), toPlan.getId(), type, amountCents, now));
    }
}
