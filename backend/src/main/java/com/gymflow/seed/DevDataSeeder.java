package com.gymflow.seed;

import com.gymflow.auth.Role;
import com.gymflow.auth.User;
import com.gymflow.auth.UserRepository;
import com.gymflow.booking.BookingService;
import com.gymflow.booking.ClassSession;
import com.gymflow.booking.ClassSessionRepository;
import com.gymflow.plan.Membership;
import com.gymflow.plan.MembershipRepository;
import com.gymflow.plan.MembershipStatus;
import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanCode;
import com.gymflow.plan.PlanRepository;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos de ejemplo del tenant único "olympusgym": owner, staff, socios, tarifas, horario de
 * las próximas dos semanas y algunas reservas (LOGIC.md §1). Solo corre en los perfiles
 * dev/demo y nunca en producción.
 *
 * <p>Las cuentas y las tarifas son idempotentes (se pueden recrear en cada arranque sin
 * duplicar). El horario y las reservas de ejemplo, en cambio, solo se generan **una vez**
 * (si ya hay alguna sesión de clase, no se tocan): sus fechas son relativas a "ahora", así
 * que regenerarlas en cada arranque las dejaría con fechas inconsistentes.
 */
@Component
@Profile({"dev", "demo"})
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    /** Contraseña de demo compartida por todas las cuentas del seed. Solo existe en dev/demo. */
    private static final String DEMO_PASSWORD = "Demo1234!";

    private record MemberSeed(String email, String fullName, PlanCode plan) {
    }

    private static final List<MemberSeed> MEMBERS = List.of(
        new MemberSeed("carlos@olympusgym.test", "Carlos M.", PlanCode.ATLETA_PRO),
        new MemberSeed("laura@olympusgym.test", "Laura Pérez", PlanCode.COMPLETA),
        new MemberSeed("marcos@olympusgym.test", "Marcos Iglesias", PlanCode.BASICA),
        new MemberSeed("sofia@olympusgym.test", "Sofía Navarro", PlanCode.COMPLETA),
        new MemberSeed("diego@olympusgym.test", "Diego Torres", PlanCode.BASICA),
        new MemberSeed("elena@olympusgym.test", "Elena Ruiz", PlanCode.ATLETA_PRO),
        new MemberSeed("pablo@olympusgym.test", "Pablo Serrano", PlanCode.COMPLETA),
        new MemberSeed("lucia@olympusgym.test", "Lucía Gil", PlanCode.BASICA),
        new MemberSeed("alvaro@olympusgym.test", "Álvaro Campos", PlanCode.COMPLETA),
        new MemberSeed("nerea@olympusgym.test", "Nerea Vidal", PlanCode.BASICA),
        new MemberSeed("hugo@olympusgym.test", "Hugo Santos", PlanCode.ATLETA_PRO),
        new MemberSeed("claudia@olympusgym.test", "Claudia Reyes", PlanCode.COMPLETA));

    /** discipline, nombre, hora, duracion en minutos, sala, aforo habitual. */
    private record SessionTemplate(
        String discipline, String name, LocalTime startTime, int durationMinutes, String room, int capacity) {
    }

    private static final List<SessionTemplate> DAILY_SESSIONS = List.of(
        new SessionTemplate("CrossFit", "CrossFit Metcon", LocalTime.of(7, 0), 60, "Box Principal", 20),
        new SessionTemplate("Fuerza", "Fuerza Básica y Técnica", LocalTime.of(9, 30), 60, "Sala Central", 16),
        new SessionTemplate("Yoga", "Yoga Dinámico y Movilidad", LocalTime.of(12, 0), 60, "Estudio Zen", 15),
        new SessionTemplate("Boxeo", "Boxeo Técnico y Saco", LocalTime.of(18, 30), 60, "Ring y Zona Funcional", 16),
        new SessionTemplate("Spinning", "Spinning Frecuencia Alta", LocalTime.of(19, 0), 60, "Sala Ciclo 1", 20),
        new SessionTemplate("Halterofilia", "Halterofilia: Técnica y Tirones", LocalTime.of(20, 0), 60, "Plataformas Olímpicas", 12));

    private static final int SCHEDULE_DAYS = 14;
    private static final ZoneOffset DEMO_ZONE = ZoneOffset.UTC;

    private final TenantRepository tenants;
    private final UserRepository users;
    private final PlanRepository plans;
    private final MembershipRepository memberships;
    private final ClassSessionRepository classSessions;
    private final BookingService bookingService;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(
            TenantRepository tenants,
            UserRepository users,
            PlanRepository plans,
            MembershipRepository memberships,
            ClassSessionRepository classSessions,
            BookingService bookingService,
            PasswordEncoder passwordEncoder) {
        this.tenants = tenants;
        this.users = users;
        this.plans = plans;
        this.memberships = memberships;
        this.classSessions = classSessions;
        this.bookingService = bookingService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Tenant tenant = tenants.findBySlug(Tenant.DEFAULT_SLUG)
            .orElseGet(() -> tenants.save(new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG)));

        Plan basica = ensurePlan(tenant, PlanCode.BASICA, "Tarifa Básica", 3900, 4, 7, false, false);
        Plan completa = ensurePlan(tenant, PlanCode.COMPLETA, "Tarifa Completa", 5900, null, 14, true, true);
        Plan pro = ensurePlan(tenant, PlanCode.ATLETA_PRO, "Tarifa Atleta Pro", 7900, null, 14, true, true);

        ensureUser(tenant, "javier@olympusgym.test", "Javier M.", Role.OWNER);
        User marta = ensureUser(tenant, "marta@olympusgym.test", "Marta Gómez", Role.STAFF);
        User dani = ensureUser(tenant, "dani@olympusgym.test", "Dani Rodríguez", Role.STAFF);

        List<User> members = MEMBERS.stream()
            .map(seed -> {
                User member = ensureUser(tenant, seed.email(), seed.fullName(), Role.MEMBER);
                Plan plan = switch (seed.plan()) {
                    case BASICA -> basica;
                    case COMPLETA -> completa;
                    case ATLETA_PRO -> pro;
                };
                ensureMembership(tenant, member, plan);
                return member;
            })
            .toList();

        if (classSessions.count() == 0) {
            seedSchedule(tenant, marta, dani, members);
        }

        log.info(
            "Seed de dev/demo comprobado: tenant={}, usuarios={}, planes={}, sesiones={}",
            tenant.getSlug(),
            users.count(),
            plans.count(),
            classSessions.count());
    }

    /** Horario de las próximas dos semanas, con una sesión a propósito completa y con lista de espera. */
    private void seedSchedule(Tenant tenant, User marta, User dani, List<User> members) {
        LocalDate today = LocalDate.now(DEMO_ZONE);

        for (int dayOffset = 0; dayOffset < SCHEDULE_DAYS; dayOffset++) {
            LocalDate day = today.plusDays(dayOffset);
            for (int i = 0; i < DAILY_SESSIONS.size(); i++) {
                SessionTemplate template = DAILY_SESSIONS.get(i);
                User coach = (dayOffset + i) % 2 == 0 ? marta : dani;
                Instant startsAt = day.atTime(template.startTime()).toInstant(DEMO_ZONE);
                Instant endsAt = startsAt.plusSeconds(template.durationMinutes() * 60L);

                // La segunda sesión de Spinning de la quincena se deja a propósito con poco
                // aforo para poder enseñar una clase completa con lista de espera.
                boolean isShowcaseWaitlistSession = dayOffset == 1 && template.discipline().equals("Spinning");
                int capacity = isShowcaseWaitlistSession ? 7 : template.capacity();

                ClassSession session = new ClassSession(
                    tenant.getId(),
                    template.name(),
                    template.discipline(),
                    "Sesión de ejemplo de " + template.discipline().toLowerCase() + ".",
                    coach.getId(),
                    template.room(),
                    startsAt,
                    endsAt,
                    capacity);
                classSessions.save(session);

                if (isShowcaseWaitlistSession) {
                    bookShowcaseSession(tenant, session, members);
                }
            }
        }

        bookAFewRegularSessions(tenant, members);
    }

    /** Reserva a 10 de los 12 socios de ejemplo: 7 confirmadas y 3 en lista de espera. */
    private void bookShowcaseSession(Tenant tenant, ClassSession session, List<User> members) {
        members.stream().limit(10).forEach(member -> tryBook(tenant, member, session.getId()));
    }

    /** Un puñado de reservas sueltas en días distintos, para que "Mis reservas" no esté vacío en la demo. */
    private void bookAFewRegularSessions(Tenant tenant, List<User> members) {
        LocalDate today = LocalDate.now(DEMO_ZONE);
        List<ClassSession> upcoming = classSessions.search(
            tenant.getId(),
            today.atStartOfDay(DEMO_ZONE).toInstant(),
            today.plusDays(SCHEDULE_DAYS).atStartOfDay(DEMO_ZONE).toInstant(),
            null,
            null,
            null);

        record ExtraBooking(String memberEmail, String discipline, int dayOffsetAtLeast) {
        }
        List<ExtraBooking> extras = List.of(
            new ExtraBooking("carlos@olympusgym.test", "CrossFit", 2),
            new ExtraBooking("laura@olympusgym.test", "Yoga", 1),
            new ExtraBooking("sofia@olympusgym.test", "Boxeo", 3));

        for (ExtraBooking extra : extras) {
            members.stream()
                .filter(m -> m.getEmail().equalsIgnoreCase(extra.memberEmail()))
                .findFirst()
                .ifPresent(member -> upcoming.stream()
                    .filter(s -> s.getDiscipline().equals(extra.discipline())
                        && s.getStartsAt().isAfter(today.plusDays(extra.dayOffsetAtLeast()).atStartOfDay(DEMO_ZONE).toInstant()))
                    .findFirst()
                    .ifPresent(session -> tryBook(tenant, member, session.getId())));
        }
    }

    private void tryBook(Tenant tenant, User member, UUID sessionId) {
        try {
            bookingService.book(tenant.getId(), member.getId(), sessionId);
        } catch (RuntimeException e) {
            log.warn("No se pudo crear la reserva de ejemplo para {} en la sesión {}: {}", member.getEmail(), sessionId, e.getMessage());
        }
    }

    private Plan ensurePlan(
            Tenant tenant,
            PlanCode code,
            String name,
            int priceCents,
            Integer classesPerMonth,
            int bookingWindowDays,
            boolean aiTrainer,
            boolean aiAssistant) {
        return plans.findByTenantIdAndCode(tenant.getId(), code)
            .orElseGet(() -> plans.save(new Plan(
                tenant.getId(), code, name, priceCents, classesPerMonth, bookingWindowDays, aiTrainer, aiAssistant)));
    }

    private User ensureUser(Tenant tenant, String email, String fullName, Role role) {
        if (users.existsByTenantIdAndEmailIgnoreCase(tenant.getId(), email)) {
            return users.findByEmailIgnoreCase(email).orElseThrow();
        }
        User user = new User(tenant.getId(), email, passwordEncoder.encode(DEMO_PASSWORD), fullName, role);
        return users.save(user);
    }

    private void ensureMembership(Tenant tenant, User member, Plan plan) {
        boolean exists = memberships.findFirstByTenantIdAndUserIdOrderByStartedAtDesc(tenant.getId(), member.getId())
            .isPresent();
        if (!exists) {
            memberships.save(
                new Membership(tenant.getId(), member.getId(), plan.getId(), MembershipStatus.ACTIVE, Instant.now()));
        }
    }
}
