package com.gymflow.plan;

import java.time.Instant;

/**
 * Avisos de cuota y tarifa (Trozo 9b, LOGIC.md §12). Misma idea que {@code booking.Notifier}:
 * única implementación del MVP escribe en log; email/SMTP se añadiría implementando esta misma
 * interfaz, sin tocar el resto.
 */
public interface MembershipNotifier {

    /** Unos días antes de la renovación, una sola vez por periodo. */
    void renewalUpcoming(Membership membership, Plan currentPlan, Instant renewsAt);

    /** El job diario renovó la membresía (cobro simulado) para el periodo siguiente. */
    void renewed(Membership membership, Plan plan, int amountCents);
}
