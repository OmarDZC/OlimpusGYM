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
import com.gymflow.workout.PlanSource;
import com.gymflow.workout.WorkoutLog;
import com.gymflow.workout.WorkoutLogRepository;
import com.gymflow.workout.WorkoutPlan;
import com.gymflow.workout.WorkoutPlanRepository;
import com.gymflow.workout.WorkoutService;
import com.gymflow.workout.dto.PlanDay;
import com.gymflow.workout.dto.PlanExercise;
import com.gymflow.workout.dto.WeeklyPlan;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos de ejemplo del tenant único "olympusgym": owner, staff, socios, horario de las
 * próximas dos semanas y algunas reservas (LOGIC.md §1). Solo corre en los perfiles dev/demo y
 * nunca en producción. El tenant y las 3 tarifas reales ya no los crea esta clase (Trozo
 * 8a-2): los crea {@link BusinessDataSeeder}, que corre en todos los perfiles y antes que este
 * (@Order).
 *
 * <p>Las cuentas son idempotentes (se pueden recrear en cada arranque sin duplicar). El
 * horario y las reservas de ejemplo, en cambio, solo se generan **una vez** (si ya hay alguna
 * sesión de clase, no se tocan): sus fechas son relativas a "ahora", así que regenerarlas en
 * cada arranque las dejaría con fechas inconsistentes.
 */
@Component
@Profile({"dev", "demo"})
@Order(10)
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    /** Contraseña de demo compartida por todas las cuentas del seed. Solo existe en dev/demo. */
    private static final String DEMO_PASSWORD = "Demo1234!";

    /**
     * Cuenta de administrador para desarrollo local (Bloque 3): correo y contraseña fáciles de
     * recordar, pensada para entrar rápido sin memorizar ninguna de las cuentas de ejemplo.
     * Mismo perfil dev/demo que el resto del seed, nunca en producción: el admin real de
     * producción (Trozo 8) se crea con las variables de entorno ADMIN_EMAIL/ADMIN_PASSWORD y
     * una contraseña fuerte, no con esto (ver docs/PROGRESS.md).
     */
    private static final String DEV_ADMIN_EMAIL = "omaradmin@gmail.com";
    private static final String DEV_ADMIN_PASSWORD = "omaradmin";

    /** Mínimo exigido a DEMO_OWNER_PASSWORD, igual que al admin real de producción. */
    private static final int MIN_DEMO_OWNER_PASSWORD_LENGTH = 12;

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

    /** Trozo 9f: en modo demo solo existen estos 3 socios (uno por tarifa), para que
     * scripts/reset-demo.sh pueda dejar un conjunto exacto y conocido de cuentas. */
    private static final List<MemberSeed> DEMO_MEMBERS = List.of(
        new MemberSeed("carlos@olympusgym.test", "Carlos M.", PlanCode.ATLETA_PRO),
        new MemberSeed("laura@olympusgym.test", "Laura Pérez", PlanCode.COMPLETA),
        new MemberSeed("marcos@olympusgym.test", "Marcos Iglesias", PlanCode.BASICA));

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
    private final WorkoutPlanRepository workoutPlans;
    private final WorkoutLogRepository workoutLogs;
    private final WorkoutService workoutService;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final String demoOwnerEmail;
    private final String demoOwnerPassword;

    public DevDataSeeder(
            TenantRepository tenants,
            UserRepository users,
            PlanRepository plans,
            MembershipRepository memberships,
            ClassSessionRepository classSessions,
            BookingService bookingService,
            WorkoutPlanRepository workoutPlans,
            WorkoutLogRepository workoutLogs,
            WorkoutService workoutService,
            PasswordEncoder passwordEncoder,
            Environment environment,
            @Value("${gymflow.demo.owner-email:}") String demoOwnerEmail,
            @Value("${gymflow.demo.owner-password:}") String demoOwnerPassword) {
        this.tenants = tenants;
        this.users = users;
        this.plans = plans;
        this.memberships = memberships;
        this.classSessions = classSessions;
        this.bookingService = bookingService;
        this.workoutPlans = workoutPlans;
        this.workoutLogs = workoutLogs;
        this.workoutService = workoutService;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.demoOwnerEmail = demoOwnerEmail;
        this.demoOwnerPassword = demoOwnerPassword;
    }

    /** {@code true} con el perfil "demo" activo (despliegue público de ejemplo): mismos datos
     * que "dev" (horario, reservas), pero sin la cuenta de admin de desarrollo, con solo 3
     * socios (no 12) y con un único OWNER real que sale de
     * {@code DEMO_OWNER_EMAIL}/{@code DEMO_OWNER_PASSWORD} (nunca en los botones de acceso ni
     * en el README) en vez del "javier@olympusgym.test" fijo de dev. */
    private boolean isDemoMode() {
        return environment.matchesProfiles("demo");
    }

    @Override
    @Transactional
    public void run(String... args) {
        // El tenant y las 3 tarifas ya existen: los crea BusinessDataSeeder (@Order(0), corre
        // en todos los perfiles, antes que este). Aquí solo se buscan.
        Tenant tenant = tenants.findBySlug(Tenant.DEFAULT_SLUG)
            .orElseThrow(() -> new IllegalStateException(
                "No existe el tenant por defecto: BusinessDataSeeder debería haberlo creado antes."));
        Plan basica = findPlan(tenant, PlanCode.BASICA);
        Plan completa = findPlan(tenant, PlanCode.COMPLETA);
        Plan pro = findPlan(tenant, PlanCode.ATLETA_PRO);

        boolean demoMode = isDemoMode();
        if (demoMode) {
            if (demoOwnerEmail.isBlank() || demoOwnerPassword.isBlank()) {
                throw new IllegalStateException("DEMO_OWNER_EMAIL y DEMO_OWNER_PASSWORD son obligatorios en modo demo.");
            }
            if (demoOwnerPassword.length() < MIN_DEMO_OWNER_PASSWORD_LENGTH) {
                throw new IllegalStateException(
                    "DEMO_OWNER_PASSWORD es demasiado corta (mínimo " + MIN_DEMO_OWNER_PASSWORD_LENGTH + " caracteres).");
            }
            // El único OWNER real de la demo: no sale en los botones de /acceso ni en el
            // README, a diferencia de las cuentas públicas de abajo.
            ensureUser(tenant, demoOwnerEmail, "Propietario", Role.OWNER, demoOwnerPassword);
        } else {
            ensureUser(tenant, "javier@olympusgym.test", "Javier M.", Role.OWNER);
            // El admin de desarrollo (contraseña corta, conocida y publicada en el README) no
            // tiene sitio en un despliegue público, ni siquiera en la demo.
            ensureUser(tenant, DEV_ADMIN_EMAIL, "Omar (admin de desarrollo)", Role.OWNER, DEV_ADMIN_PASSWORD);
        }
        User marta = ensureUser(tenant, "marta@olympusgym.test", "Marta Gómez", Role.STAFF);
        User dani = ensureUser(tenant, "dani@olympusgym.test", "Dani Rodríguez", Role.STAFF);

        // En demo, solo 3 socios (uno por tarifa) con la misma contraseña simple que el resto
        // de cuentas públicas: scripts/reset-demo.sh puede entonces dejar un conjunto exacto y
        // conocido. En dev, los 12 de siempre, para una demo local más rica.
        List<MemberSeed> membersToSeed = demoMode ? DEMO_MEMBERS : MEMBERS;
        List<User> members = membersToSeed.stream()
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

        members.stream()
            .filter(m -> m.getEmail().equalsIgnoreCase("carlos@olympusgym.test"))
            .findFirst()
            .ifPresent(carlos -> seedCarlosWorkoutPlan(tenant, carlos));

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
                // aforo para poder enseñar una clase completa con lista de espera. En demo
                // solo hay 3 socios (no 12): con aforo 7 nadie llegaría a la lista de espera.
                boolean isShowcaseWaitlistSession = dayOffset == 1 && template.discipline().equals("Spinning");
                int capacity = isShowcaseWaitlistSession ? (isDemoMode() ? 2 : 7) : template.capacity();

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
        List<ExtraBooking> extras = isDemoMode()
            ? List.of(
                new ExtraBooking("carlos@olympusgym.test", "CrossFit", 2),
                new ExtraBooking("laura@olympusgym.test", "Yoga", 1),
                new ExtraBooking("marcos@olympusgym.test", "Boxeo", 3))
            : List.of(
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

    /**
     * Rutina de ejemplo de Carlos (LOGIC.md §1), 5 días a la semana. Se guarda como plan
     * MANUAL: la generación real con IA llega en el Trozo 5. Si ya tiene un plan para la
     * semana actual, no se toca (así cada semana que arranques en dev tendrás una rutina
     * fresca, pero sin perder lo que ya hubiera para la semana en curso).
     */
    private void seedCarlosWorkoutPlan(Tenant tenant, User carlos) {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Madrid"));
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (workoutPlans.findByTenantIdAndUserIdAndWeekStart(tenant.getId(), carlos.getId(), weekStart).isPresent()) {
            return;
        }

        WeeklyPlan weeklyPlan = new WeeklyPlan(List.of(
            new PlanDay(0, "Día 1: Empuje y espalda alta", "Fuerza e hipertrofia", false, List.of(
                new PlanExercise(0, "Press de banca plano", 4, "8-10", 80.0, 90,
                    "Barra olímpica, agarre cerrado con pausa escapular"),
                new PlanExercise(1, "Press militar con mancuernas", 3, "10", 24.0, 75, "Banco a 75°"),
                new PlanExercise(2, "Fondos en paralelas", 3, "8-12", null, 75, "Lastre opcional"),
                new PlanExercise(3, "Extensión de tríceps en polea", 3, "12-15", 25.0, 60, "Agarre prono"))),
            new PlanDay(1, "Día 2: Espalda y tirón", "Fuerza e hipertrofia", false, List.of(
                new PlanExercise(0, "Dominadas con lastre", 4, "6-8", 10.0, 90,
                    "Agarre prono, extensión completa de codo"),
                new PlanExercise(1, "Remo con barra 45°", 4, "10", 70.0, 75, "Tracción al esternón"),
                new PlanExercise(2, "Curl de bíceps con barra", 3, "10-12", 30.0, 60, null),
                new PlanExercise(3, "Face pull en polea", 3, "15", 15.0, 45, "Codos altos"))),
            new PlanDay(2, "Día 3: Descanso", "Recuperación", true, List.of()),
            new PlanDay(3, "Día 4: Pierna completa", "Fuerza e hipertrofia", false, List.of(
                new PlanExercise(0, "Sentadilla trasera barra alta", 4, "6", 105.0, 120,
                    "Profundidad paralela con control en el descenso"),
                new PlanExercise(1, "Peso muerto rumano", 3, "8-10", 90.0, 90,
                    "Espalda neutra, rango desde la cadera"),
                new PlanExercise(2, "Zancadas con mancuernas", 3, "12 por pierna", 20.0, 75, null),
                new PlanExercise(3, "Elevación de gemelos de pie", 4, "15-20", 40.0, 45, null))),
            new PlanDay(4, "Día 5: Empuje ligero y core", "Técnica y control", false, List.of(
                new PlanExercise(0, "Press inclinado con mancuernas", 3, "10", 28.0, 75, "Banco a 30°"),
                new PlanExercise(1, "Remo con mancuerna a una mano", 3, "10 por brazo", 26.0, 60, null),
                new PlanExercise(2, "Plancha", 3, "45 seg", null, 45, "Core activo, cadera neutra"))),
            new PlanDay(5, "Día 6: Acondicionamiento", "Resistencia y fuerza general", false, List.of(
                new PlanExercise(0, "Sentadilla goblet", 3, "12", 24.0, 60, null),
                new PlanExercise(1, "Dominadas asistidas", 3, "8-10", null, 60, "Banda de asistencia según nivel"),
                new PlanExercise(2, "Burpees", 3, "12", null, 45, null),
                new PlanExercise(3, "Plancha lateral", 3, "30 seg por lado", null, 45, null))),
            new PlanDay(6, "Día 7: Descanso", "Recuperación", true, List.of())));

        WorkoutPlan plan = new WorkoutPlan(
            tenant.getId(), carlos.getId(), weekStart, "Fuerza e hipertrofia", "Avanzado", 5,
            workoutService.serialize(weeklyPlan), PlanSource.MANUAL);
        workoutPlans.save(plan);

        // Días de entrenamiento ya pasados esta semana: se marcan como completados para que
        // la constancia semanal no empiece siempre a cero en la demo.
        int todayIndex = today.getDayOfWeek().getValue() - 1;
        for (PlanDay day : weeklyPlan.days()) {
            if (day.restDay() || day.dayIndex() >= todayIndex) {
                continue;
            }
            for (PlanExercise exercise : day.exercises()) {
                for (int setIndex = 0; setIndex < exercise.sets(); setIndex++) {
                    BigDecimal weight = exercise.weightKg() != null ? BigDecimal.valueOf(exercise.weightKg()) : null;
                    workoutLogs.save(new WorkoutLog(
                        tenant.getId(), carlos.getId(), plan.getId(), day.dayIndex(), exercise.exerciseIndex(),
                        setIndex, true, weight, null));
                }
            }
        }
    }

    private void tryBook(Tenant tenant, User member, UUID sessionId) {
        try {
            bookingService.book(tenant.getId(), member.getId(), sessionId);
        } catch (RuntimeException e) {
            log.warn("No se pudo crear la reserva de ejemplo para {} en la sesión {}: {}", member.getEmail(), sessionId, e.getMessage());
        }
    }

    private Plan findPlan(Tenant tenant, PlanCode code) {
        return plans.findByTenantIdAndCode(tenant.getId(), code)
            .orElseThrow(() -> new IllegalStateException(
                "No existe la tarifa " + code + ": BusinessDataSeeder debería haberla creado antes."));
    }

    /** Staff y socios, en dev y en demo por igual, usan la misma contraseña simple y
     * pública: el único secreto real de la demo es el OWNER (ver más arriba). */
    private User ensureUser(Tenant tenant, String email, String fullName, Role role) {
        return ensureUser(tenant, email, fullName, role, DEMO_PASSWORD);
    }

    /** Igual que {@link #ensureUser(Tenant, String, String, Role)}, pero con una contraseña propia
     * en vez de la compartida de demo (p. ej. el admin de desarrollo del Bloque 3). Mismo BCrypt
     * que todas las demás: solo cambia el texto en claro que se cifra. */
    private User ensureUser(Tenant tenant, String email, String fullName, Role role, String rawPassword) {
        if (users.existsByTenantIdAndEmailIgnoreCase(tenant.getId(), email)) {
            return users.findByEmailIgnoreCase(email).orElseThrow();
        }
        User user = new User(tenant.getId(), email, passwordEncoder.encode(rawPassword), fullName, role);
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
