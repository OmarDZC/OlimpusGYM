package com.gymflow.plan;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findFirstByTenantIdAndUserIdOrderByStartedAtDesc(UUID tenantId, UUID userId);

    List<Membership> findByTenantIdAndStatus(UUID tenantId, MembershipStatus status);

    List<Membership> findByTenantId(UUID tenantId);

    /** Periodos ya terminados: los recoge el job diario de renovación (Trozo 9b). */
    List<Membership> findByStatusAndPeriodEndLessThanEqual(MembershipStatus status, Instant now);

    /** Para el aviso de renovación: dentro de la ventana y todavía sin avisar en este periodo. */
    List<Membership> findByStatusAndPeriodEndBetweenAndRenewalNoticeSentAtIsNull(
            MembershipStatus status, Instant from, Instant to);
}
