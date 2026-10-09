package com.gymflow.plan;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogMembershipNotifier implements MembershipNotifier {

    private static final Logger log = LoggerFactory.getLogger(LogMembershipNotifier.class);

    @Override
    public void renewalUpcoming(Membership membership, Plan currentPlan, Instant renewsAt) {
        log.info(
                "[notificacion] Tu suscripcion se renueva el {} por {} EUR/mes: usuario={} tarifa={}",
                renewsAt, currentPlan.getPriceCents() / 100.0, membership.getUserId(), currentPlan.getName());
    }

    @Override
    public void renewed(Membership membership, Plan plan, int amountCents) {
        log.info(
                "[notificacion] Renovacion simulada: usuario={} tarifa={} importe={} centimos, periodo hasta {}",
                membership.getUserId(), plan.getName(), amountCents, membership.getPeriodEnd());
    }
}
