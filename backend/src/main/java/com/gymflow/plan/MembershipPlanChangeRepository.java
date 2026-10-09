package com.gymflow.plan;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipPlanChangeRepository extends JpaRepository<MembershipPlanChange, UUID> {

    List<MembershipPlanChange> findByMembershipIdOrderByCreatedAtDesc(UUID membershipId);
}
