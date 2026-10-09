package com.gymflow.admin;

import com.gymflow.admin.dto.MemberSummary;
import com.gymflow.auth.Role;
import com.gymflow.auth.User;
import com.gymflow.auth.UserRepository;
import com.gymflow.plan.Membership;
import com.gymflow.plan.MembershipPlanService;
import com.gymflow.plan.MembershipRepository;
import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Listado de socios y cambio de tarifa (LOGIC.md §6). */
@Service
public class AdminMemberService {

    private final UserRepository users;
    private final MembershipRepository memberships;
    private final PlanRepository plans;
    private final MembershipPlanService membershipPlanService;

    public AdminMemberService(
            UserRepository users,
            MembershipRepository memberships,
            PlanRepository plans,
            MembershipPlanService membershipPlanService) {
        this.users = users;
        this.memberships = memberships;
        this.plans = plans;
        this.membershipPlanService = membershipPlanService;
    }

    /** {@code includePricing}: solo OWNER ve el precio de la tarifa de cada socio (LOGIC.md §2). */
    @Transactional(readOnly = true)
    public List<MemberSummary> list(UUID tenantId, boolean includePricing) {
        List<User> members = users.findByTenantIdAndRole(tenantId, Role.MEMBER);
        List<Membership> allMemberships = memberships.findByTenantId(tenantId);
        Map<UUID, Membership> membershipByUser = allMemberships.stream()
                .collect(Collectors.toMap(Membership::getUserId, m -> m, (a, b) -> a));
        Map<UUID, Plan> planById = plans.findAllById(allMemberships.stream().map(Membership::getPlanId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Plan::getId, p -> p));

        return members.stream()
                .map(member -> toSummary(member, membershipByUser.get(member.getId()), planById, includePricing))
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberSummary detail(UUID tenantId, UUID memberId, boolean includePricing) {
        User member = users.findById(memberId)
                .filter(u -> u.getTenantId().equals(tenantId) && u.getRole() == Role.MEMBER)
                .orElseThrow(() -> new IllegalArgumentException("No existe ese socio"));
        Membership membership = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenantId, memberId)
                .orElse(null);
        Plan plan = membership != null ? plans.findById(membership.getPlanId()).orElse(null) : null;
        return toSummary(member, membership, plan, includePricing);
    }

    /**
     * Solo OWNER (SecurityConfig: PATCH .../admin/members/{id}/plan). Pasa por las mismas reglas
     * de subir/bajar de tarifa que el autoservicio del socio (Trozo 9b), para que los periodos
     * nunca se desincronicen según quién haga el cambio: subir se aplica al instante y prorratea;
     * bajar se programa para la siguiente renovación, no se aplica ya.
     */
    @Transactional
    public MemberSummary changePlan(UUID tenantId, UUID ownerUserId, UUID memberId, com.gymflow.plan.PlanCode newPlanCode, boolean includePricing) {
        membershipPlanService.changePlanAsOwner(tenantId, ownerUserId, memberId, newPlanCode);

        User member = users.findById(memberId).orElseThrow(() -> new IllegalArgumentException("No existe ese socio"));
        Membership membership = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenantId, memberId)
                .orElse(null);
        Plan plan = membership != null ? plans.findById(membership.getPlanId()).orElse(null) : null;
        return toSummary(member, membership, plan, includePricing);
    }

    private MemberSummary toSummary(User member, Membership membership, Map<UUID, Plan> planById, boolean includePricing) {
        Plan plan = membership != null ? planById.get(membership.getPlanId()) : null;
        return toSummary(member, membership, plan, includePricing);
    }

    private MemberSummary toSummary(User member, Membership membership, Plan plan, boolean includePricing) {
        return new MemberSummary(
                member.getId(),
                member.getFullName(),
                member.getEmail(),
                plan != null ? plan.getCode().name() : null,
                plan != null ? plan.getName() : null,
                membership != null ? membership.getStatus().name() : null,
                includePricing && plan != null ? plan.getPriceCents() : null);
    }
}
