package com.gymflow.booking;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID> {

    /**
     * Bloquea la fila de la sesión (SELECT ... FOR UPDATE) para toda la duración de la
     * transacción que reserva. Es la base del aforo sin sobreventa (LOGIC.md §5.1): dos
     * reservas concurrentes para la misma sesión se serializan aquí, así que el recuento
     * de plazas ocupadas que se lea después siempre es correcto.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ClassSession s where s.id = :id and s.tenantId = :tenantId")
    Optional<ClassSession> lockById(@Param("tenantId") UUID tenantId, @Param("id") UUID id);

    Optional<ClassSession> findByIdAndTenantId(UUID id, UUID tenantId);

    // "cast(:param as string)" evita que el driver de Postgres falle al resolver el tipo de
    // un parámetro null pasado directamente a lower() (JDBC no puede inferirlo sin pista).
    @Query("""
        select s from ClassSession s
        where s.tenantId = :tenantId
          and s.status = com.gymflow.booking.SessionStatus.SCHEDULED
          and s.startsAt >= :from and s.startsAt < :to
          and (:discipline is null or lower(s.discipline) = lower(cast(:discipline as string)))
          and (:coachId is null or s.coachUserId = :coachId)
          and (:room is null or lower(s.room) = lower(cast(:room as string)))
        order by s.startsAt asc
        """)
    List<ClassSession> search(
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("discipline") String discipline,
            @Param("coachId") UUID coachId,
            @Param("room") String room);
}
