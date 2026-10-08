package com.gymflow.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanCode code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    /** NULL = clases ilimitadas. */
    @Column(name = "classes_per_month")
    private Integer classesPerMonth;

    @Column(name = "booking_window_days", nullable = false)
    private int bookingWindowDays;

    @Column(name = "ai_trainer", nullable = false)
    private boolean aiTrainer;

    @Column(name = "ai_assistant", nullable = false)
    private boolean aiAssistant;

    @Column(nullable = false)
    private boolean active = true;

    protected Plan() {
    }

    public Plan(
            UUID tenantId,
            PlanCode code,
            String name,
            int priceCents,
            Integer classesPerMonth,
            int bookingWindowDays,
            boolean aiTrainer,
            boolean aiAssistant) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
        this.priceCents = priceCents;
        this.classesPerMonth = classesPerMonth;
        this.bookingWindowDays = bookingWindowDays;
        this.aiTrainer = aiTrainer;
        this.aiAssistant = aiAssistant;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public PlanCode getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public Integer getClassesPerMonth() {
        return classesPerMonth;
    }

    public int getBookingWindowDays() {
        return bookingWindowDays;
    }

    public boolean isAiTrainer() {
        return aiTrainer;
    }

    public boolean isAiAssistant() {
        return aiAssistant;
    }

    public boolean isActive() {
        return active;
    }
}
