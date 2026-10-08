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
@Table(name = "bookings")
public class Booking {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "waitlist_position")
    private Integer waitlistPosition;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Booking() {
    }

    public Booking(UUID tenantId, UUID sessionId, UUID userId, BookingStatus status, Integer waitlistPosition) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.status = status;
        this.waitlistPosition = waitlistPosition;
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    public void promoteToBooked() {
        this.status = BookingStatus.BOOKED;
        this.waitlistPosition = null;
    }

    public void decrementWaitlistPosition() {
        if (this.waitlistPosition != null) {
            this.waitlistPosition -= 1;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Integer getWaitlistPosition() {
        return waitlistPosition;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }
}
