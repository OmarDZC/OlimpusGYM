package com.gymflow.admin;

import com.gymflow.admin.dto.AdminSummaryResponse;
import com.gymflow.admin.dto.MemberToContact;
import com.gymflow.auth.UserRepository;
import com.gymflow.booking.BookingRepository;
import com.gymflow.booking.BookingStatus;
import com.gymflow.booking.ClassSession;
import com.gymflow.booking.ClassSessionRepository;
import com.gymflow.plan.Membership;
import com.gymflow.plan.MembershipRepository;
import com.gymflow.plan.MembershipStatus;
import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resumen del panel de gestión (LOGIC.md §6: GET /admin/summary). */
@Service
public class AdminSummaryService {

    private static final ZoneId GYM_ZONE = ZoneId.of("Europe/Madrid");

    /** "Socios que conviene contactar": sin una reserva de verdad en los últimos 14 días. */
    private static final int CONTACT_THRESHOLD_DAYS = 14;

    private final MembershipRepository memberships;
    private final PlanRepository plans;
    private final ClassSessionRepository sessions;
    private final BookingRepository bookings;
    private final UserRepository users;

    public AdminSummaryService(
            MembershipRepository memberships,
            PlanRepository plans,
            ClassSessionRepository sessions,
            BookingRepository bookings,
            UserRepository users) {
        this.memberships = memberships;
        this.plans = plans;
        this.sessions = sessions;
        this.bookings = bookings;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public AdminSummaryResponse summary(UUID tenantId, boolean includeRevenue) {
        List<Membership> activeMemberships = memberships.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);

        Integer monthlyRevenueCents = null;
        if (includeRevenue) {
            Map<UUID, Plan> planById = plans
                    .findAllById(activeMemberships.stream().map(Membership::getPlanId).distinct().toList())
                    .stream()
                    .collect(Collectors.toMap(Plan::getId, p -> p));
            monthlyRevenueCents = activeMemberships.stream()
                    .mapToInt(m -> {
                        Plan plan = planById.get(m.getPlanId());
                        return plan != null ? plan.getPriceCents() : 0;
                    })
                    .sum();
        }

        LocalDate today = LocalDate.now(GYM_ZONE);
        Instant dayStart = today.atStartOfDay(GYM_ZONE).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(GYM_ZONE).toInstant();
        List<ClassSession> todaysSessions = sessions.search(tenantId, dayStart, dayEnd, null, null, null);
        int todayCapacity = todaysSessions.stream().mapToInt(ClassSession::getCapacity).sum();
        int todayBookedSeats = todaysSessions.stream()
                .mapToInt(s -> (int) bookings.countBySessionIdAndStatus(s.getId(), BookingStatus.BOOKED))
                .sum();

        List<MemberToContact> toContact = findMembersToContact(tenantId, activeMemberships);

        return new AdminSummaryResponse(
                activeMemberships.size(), todaysSessions.size(), todayBookedSeats, todayCapacity,
                monthlyRevenueCents, toContact);
    }

    private List<MemberToContact> findMembersToContact(UUID tenantId, List<Membership> activeMemberships) {
        Instant threshold = Instant.now().minus(CONTACT_THRESHOLD_DAYS, ChronoUnit.DAYS);
        List<MemberToContact> result = new ArrayList<>();
        for (Membership membership : activeMemberships) {
            Optional<Instant> lastBooking = bookings.findLastBookingCreatedAt(tenantId, membership.getUserId());
            if (lastBooking.isEmpty() || lastBooking.get().isBefore(threshold)) {
                users.findById(membership.getUserId())
                        .ifPresent(u -> result.add(
                                new MemberToContact(u.getId(), u.getFullName(), u.getEmail(), lastBooking.orElse(null))));
            }
        }
        return result;
    }
}
