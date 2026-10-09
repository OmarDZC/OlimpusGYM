package com.gymflow.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "memberships")
public class Membership {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    /** Periodo actual (Trozo 9b, LOGIC.md §12): un mes natural en UTC, sin ambigüedad de DST. */
    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    /** Bajada de tarifa programada para cuando termine el periodo actual. NULL = ninguna. */
    @Column(name = "pending_plan_id")
    private UUID pendingPlanId;

    /** Para no avisar dos veces de la misma renovación. Se limpia al abrir un periodo nuevo. */
    @Column(name = "renewal_notice_sent_at")
    private Instant renewalNoticeSentAt;

    protected Membership() {
    }

    public Membership(UUID tenantId, UUID userId, UUID planId, MembershipStatus status, Instant startedAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.userId = userId;
        this.planId = planId;
        this.status = status;
        this.startedAt = startedAt;
        this.periodStart = startedAt;
        this.periodEnd = plusOneUtcMonth(startedAt);
    }

    /** Un mes natural en UTC: nunca ambiguo por el cambio de hora (que solo afecta a zonas con DST). */
    static Instant plusOneUtcMonth(Instant instant) {
        return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC).plusMonths(1).toInstant();
    }

    /**
     * Sube de tarifa al instante: el periodo no se reinicia, solo cambia qué tarifa paga.
     * Si había una bajada programada, subir la cancela (ya no tiene sentido bajar a la
     * siguiente renovación si el socio acaba de decidir pagar más).
     */
    public void applyUpgrade(UUID newPlanId) {
        this.planId = newPlanId;
        this.pendingPlanId = null;
    }

    /** Programa una bajada para la siguiente renovación; hasta entonces no cambia nada más. */
    public void scheduleDowngrade(UUID newPlanId) {
        this.pendingPlanId = newPlanId;
    }

    public void cancelScheduledDowngrade() {
        this.pendingPlanId = null;
    }

    /**
     * Renovación (automática, por el job diario, o la aplica directamente un cambio de tarifa
     * del dueño): aplica la bajada programada si había una, abre el periodo siguiente y olvida
     * el aviso de renovación ya mandado (el periodo nuevo necesita el suyo propio).
     * Devuelve la tarifa pendiente si se aplicó una bajada, para poder registrar el movimiento.
     */
    public Optional<UUID> renew(Instant now) {
        Optional<UUID> appliedPendingPlan = Optional.ofNullable(pendingPlanId);
        appliedPendingPlan.ifPresent(planId -> this.planId = planId);
        this.pendingPlanId = null;
        this.periodStart = this.periodEnd;
        this.periodEnd = plusOneUtcMonth(this.periodStart);
        this.renewalNoticeSentAt = null;
        return appliedPendingPlan;
    }

    public void markRenewalNoticeSent(Instant now) {
        this.renewalNoticeSentAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getPlanId() {
        return planId;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public UUID getPendingPlanId() {
        return pendingPlanId;
    }

    public Instant getRenewalNoticeSentAt() {
        return renewalNoticeSentAt;
    }
}
