package com.gymflow.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
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

    @Column(name = "renews_at")
    private Instant renewsAt;

    protected Membership() {
    }

    public Membership(UUID tenantId, UUID userId, UUID planId, MembershipStatus status, Instant startedAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.userId = userId;
        this.planId = planId;
        this.status = status;
        this.startedAt = startedAt;
    }

    /** Cambio de tarifa (solo OWNER, LOGIC.md §6). Se cambia la membresía en sitio, sin crear una nueva. */
    public void changePlan(UUID newPlanId) {
        this.planId = newPlanId;
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

    public Instant getRenewsAt() {
        return renewsAt;
    }
}
