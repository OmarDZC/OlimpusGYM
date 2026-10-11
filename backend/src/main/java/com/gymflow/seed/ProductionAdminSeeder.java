package com.gymflow.seed;

import com.gymflow.auth.Role;
import com.gymflow.auth.User;
import com.gymflow.auth.UserRepository;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administrador real de producción (Trozo 8a-1): fuera de dev/demo no hay seed de ejemplo
 * (ver {@link DevDataSeeder}), así que sin esto no habría ninguna cuenta OWNER con la que
 * entrar la primera vez. Crea el tenant único si todavía no existe y, si no hay ya un usuario
 * con el correo de {@code GYMFLOW_ADMIN_EMAIL}, lo da de alta como OWNER con
 * {@code GYMFLOW_ADMIN_PASSWORD}. Idempotente: en arranques siguientes no hace nada si la
 * cuenta ya existe.
 */
@Component
@Profile("!dev & !demo")
public class ProductionAdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionAdminSeeder.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final TenantRepository tenants;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public ProductionAdminSeeder(
            TenantRepository tenants,
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${gymflow.admin.email:}") String adminEmail,
            @Value("${gymflow.admin.password:}") String adminPassword) {
        this.tenants = tenants;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException(
                "GYMFLOW_ADMIN_EMAIL no está definido. Fuera de dev/demo hace falta para crear el "
                    + "administrador real.");
        }
        if (adminPassword == null || adminPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                "GYMFLOW_ADMIN_PASSWORD tiene que tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }

        Tenant tenant = tenants.findBySlug(Tenant.DEFAULT_SLUG)
            .orElseGet(() -> tenants.save(new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG)));

        if (users.existsByTenantIdAndEmailIgnoreCase(tenant.getId(), adminEmail)) {
            log.info("El administrador de producción ({}) ya existe: no se toca.", adminEmail);
            return;
        }

        users.save(new User(
            tenant.getId(), adminEmail, passwordEncoder.encode(adminPassword), "Administrador", Role.OWNER));
        log.info("Administrador de producción creado: {}", adminEmail);
    }
}
