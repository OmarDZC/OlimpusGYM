package com.gymflow.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "class_sessions")
public class ClassSession {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 60)
    private String discipline;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "coach_user_id")
    private UUID coachUserId;

    @Column(nullable = false, length = 60)
    private String room;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.SCHEDULED;

    protected ClassSession() {
    }

    public ClassSession(
            UUID tenantId,
            String name,
            String discipline,
            String description,
            UUID coachUserId,
            String room,
            Instant startsAt,
            Instant endsAt,
            int capacity) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.name = name;
        this.discipline = discipline;
        this.description = description;
        this.coachUserId = coachUserId;
        this.room = room;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.capacity = capacity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getDiscipline() {
        return discipline;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCoachUserId() {
        return coachUserId;
    }

    public String getRoom() {
        return room;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public int getCapacity() {
        return capacity;
    }

    public SessionStatus getStatus() {
        return status;
    }
}
