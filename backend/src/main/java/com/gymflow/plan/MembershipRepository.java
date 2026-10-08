package com.gymflow.plan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findFirstByTenantIdAndUserIdOrderByStartedAtDesc(UUID tenantId, UUID userId);

    List<Membership> findByTenantIdAndStatus(UUID tenantId, MembershipStatus status);

    List<Membership> findByTenantId(UUID tenantId);
}
