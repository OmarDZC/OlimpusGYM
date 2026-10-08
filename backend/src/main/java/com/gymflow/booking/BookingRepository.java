package com.gymflow.booking;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
        select b from Booking b
        where b.tenantId = :tenantId and b.userId = :userId and b.sessionId = :sessionId
          and b.status in (com.gymflow.booking.BookingStatus.BOOKED, com.gymflow.booking.BookingStatus.WAITLIST)
        """)
    Optional<Booking> findActive(
            @Param("tenantId") UUID tenantId, @Param("userId") UUID userId, @Param("sessionId") UUID sessionId);

    long countBySessionIdAndStatus(UUID sessionId, BookingStatus status);

    Optional<Booking> findFirstBySessionIdAndStatusOrderByWaitlistPositionAsc(UUID sessionId, BookingStatus status);

    List<Booking> findBySessionIdAndStatusAndWaitlistPositionGreaterThanOrderByWaitlistPositionAsc(
            UUID sessionId, BookingStatus status, int waitlistPosition);

    @Query("""
        select case when count(b) > 0 then true else false end from Booking b
        where b.tenantId = :tenantId and b.userId = :userId
          and b.status in (com.gymflow.booking.BookingStatus.BOOKED, com.gymflow.booking.BookingStatus.WAITLIST)
          and b.sessionId in (
            select s.id from ClassSession s
            where s.tenantId = :tenantId and s.startsAt < :endsAt and s.endsAt > :startsAt
          )
        """)
    boolean existsActiveOverlapping(
            @Param("tenantId") UUID tenantId,
            @Param("userId") UUID userId,
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt);

    /** Reservas BOOKED de sesiones cuyo inicio cae dentro del mes natural indicado (cupo del plan). */
    @Query("""
        select count(b) from Booking b
        where b.tenantId = :tenantId and b.userId = :userId and b.status = com.gymflow.booking.BookingStatus.BOOKED
          and b.sessionId in (
            select s.id from ClassSession s
            where s.tenantId = :tenantId and s.startsAt >= :monthStart and s.startsAt < :monthEnd
          )
        """)
    long countBookedForSessionsInMonth(
            @Param("tenantId") UUID tenantId,
            @Param("userId") UUID userId,
            @Param("monthStart") Instant monthStart,
            @Param("monthEnd") Instant monthEnd);

    @Query("""
        select b from Booking b
        where b.tenantId = :tenantId and b.userId = :userId
          and b.status in (com.gymflow.booking.BookingStatus.BOOKED, com.gymflow.booking.BookingStatus.WAITLIST)
        order by b.createdAt asc
        """)
    List<Booking> findActiveForUser(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);
}
