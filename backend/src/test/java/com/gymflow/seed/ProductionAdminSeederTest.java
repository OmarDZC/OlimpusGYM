package com.gymflow.seed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymflow.auth.Role;
import com.gymflow.auth.User;
import com.gymflow.auth.UserRepository;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Trozo 8a-1: el administrador real de producción. Test unitario con mocks (sin Spring ni
 * Testcontainers): lo que importa comprobar es la lógica propia del seeder, no el contexto de
 * Spring, que ya prueba {@code DevDataSeederProductionProfileTest} para el perfil "prod".
 */
class ProductionAdminSeederTest {

    private final TenantRepository tenants = mock(TenantRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void createsTheOwnerWhenTheTenantExistsAndTheAccountDoesNotYet() {
        // BusinessDataSeeder (Trozo 8a-2) garantiza el tenant antes de que este corra: ya no lo
        // crea él mismo, solo lo busca.
        Tenant existingTenant = new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG);
        when(tenants.findBySlug(Tenant.DEFAULT_SLUG)).thenReturn(Optional.of(existingTenant));
        when(users.existsByTenantIdAndEmailIgnoreCase(existingTenant.getId(), "admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("una-contraseña-larga")).thenReturn("hash");

        ProductionAdminSeeder seeder = new ProductionAdminSeeder(
            tenants, users, passwordEncoder, "admin@example.com", "una-contraseña-larga");
        seeder.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("admin@example.com", saved.getEmail());
        assertEquals("hash", saved.getPasswordHash());
        assertEquals(Role.OWNER, saved.getRole());
        assertEquals(existingTenant.getId(), saved.getTenantId());
    }

    @Test
    void failsClearlyWhenTheTenantDoesNotExistYet() {
        when(tenants.findBySlug(Tenant.DEFAULT_SLUG)).thenReturn(Optional.empty());

        ProductionAdminSeeder seeder = new ProductionAdminSeeder(
            tenants, users, passwordEncoder, "admin@example.com", "una-contraseña-larga");

        assertThrows(IllegalStateException.class, seeder::run);
        verify(users, never()).save(any());
    }

    @Test
    void doesNothingWhenTheAccountAlreadyExists() {
        UUID tenantId = UUID.randomUUID();
        Tenant existingTenant = new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG);
        when(tenants.findBySlug(Tenant.DEFAULT_SLUG)).thenReturn(Optional.of(existingTenant));
        when(users.existsByTenantIdAndEmailIgnoreCase(existingTenant.getId(), "admin@example.com")).thenReturn(true);

        ProductionAdminSeeder seeder = new ProductionAdminSeeder(
            tenants, users, passwordEncoder, "admin@example.com", "una-contraseña-larga");
        seeder.run();

        verify(users, never()).save(any());
    }

    @Test
    void rejectsAPasswordShorterThan12Characters() {
        ProductionAdminSeeder seeder = new ProductionAdminSeeder(
            tenants, users, passwordEncoder, "admin@example.com", "corta123456");

        assertThrows(IllegalStateException.class, seeder::run);
        verify(users, never()).save(any());
    }

    @Test
    void rejectsAMissingEmail() {
        ProductionAdminSeeder seeder = new ProductionAdminSeeder(tenants, users, passwordEncoder, "", "una-contraseña-larga");

        assertThrows(IllegalStateException.class, seeder::run);
        verify(users, never()).save(any());
    }
}
