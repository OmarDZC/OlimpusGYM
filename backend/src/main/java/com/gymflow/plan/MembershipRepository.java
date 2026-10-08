package com.gymflow.plan;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findFirstByTenantIdAndUserIdOrderByStartedAtDesc(UUID tenantId, UUID userId);
}
