package com.gymflow.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Historial de cambios de tarifa (Trozo 9b, LOGIC.md §12): quién, cuándo, de qué tarifa a cuál, importe. */
@Entity
@Table(name = "membership_plan_changes")
public class MembershipPlanChange {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "membership_id", nullable = false)
    private UUID membershipId;

    /** NULL = lo hizo el job automático de renovación, no una persona. */
    @Column(name = "changed_by_user_id")
    private UUID changedByUserId;

    @Column(name = "from_plan_id", nullable = false)
    private UUID fromPlanId;

    @Column(name = "to_plan_id", nullable = false)
    private UUID toPlanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private PlanChangeType changeType;

    /** Céntimos enteros; NULL cuando el cambio no implica cobro. */
    @Column(name = "amount_cents")
    private Integer amountCents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MembershipPlanChange() {
    }

    public MembershipPlanChange(
            UUID tenantId,
            UUID membershipId,
            UUID changedByUserId,
            UUID fromPlanId,
            UUID toPlanId,
            PlanChangeType changeType,
            Integer amountCents,
            Instant createdAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.membershipId = membershipId;
        this.changedByUserId = changedByUserId;
        this.fromPlanId = fromPlanId;
        this.toPlanId = toPlanId;
        this.changeType = changeType;
        this.amountCents = amountCents;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getMembershipId() {
        return membershipId;
    }

    public UUID getChangedByUserId() {
        return changedByUserId;
    }

    public UUID getFromPlanId() {
        return fromPlanId;
    }

    public UUID getToPlanId() {
        return toPlanId;
    }

    public PlanChangeType getChangeType() {
        return changeType;
    }

    public Integer getAmountCents() {
        return amountCents;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
