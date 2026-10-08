package com.gymflow.seed;

import com.gymflow.auth.Role;
import com.gymflow.auth.User;
import com.gymflow.auth.UserRepository;
import com.gymflow.plan.Membership;
import com.gymflow.plan.MembershipRepository;
import com.gymflow.plan.MembershipStatus;
import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanCode;
import com.gymflow.plan.PlanRepository;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos de ejemplo del tenant único "olympusgym": owner, staff, socios y tarifas
 * (LOGIC.md §1). Solo corre en los perfiles dev/demo y nunca en producción.
 * Idempotente: se puede arrancar la app muchas veces sin duplicar nada.
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

    private final TenantRepository tenants;
    private final UserRepository users;
    private final PlanRepository plans;
    private final MembershipRepository memberships;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(
            TenantRepository tenants,
            UserRepository users,
            PlanRepository plans,
            MembershipRepository memberships,
            PasswordEncoder passwordEncoder) {
        this.tenants = tenants;
        this.users = users;
        this.plans = plans;
        this.memberships = memberships;
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
        ensureUser(tenant, "marta@olympusgym.test", "Marta Gómez", Role.STAFF);
        ensureUser(tenant, "dani@olympusgym.test", "Dani Rodríguez", Role.STAFF);

        for (MemberSeed seed : MEMBERS) {
            User member = ensureUser(tenant, seed.email(), seed.fullName(), Role.MEMBER);
            Plan plan = switch (seed.plan()) {
                case BASICA -> basica;
                case COMPLETA -> completa;
                case ATLETA_PRO -> pro;
            };
            ensureMembership(tenant, member, plan);
        }

        log.info(
            "Seed de dev/demo comprobado: tenant={}, usuarios={}, planes={}",
            tenant.getSlug(),
            users.count(),
            plans.count());
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
