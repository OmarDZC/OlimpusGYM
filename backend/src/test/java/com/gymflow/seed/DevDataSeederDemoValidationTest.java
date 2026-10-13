package com.gymflow.seed;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymflow.booking.BookingService;
import com.gymflow.booking.ClassSessionRepository;
import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanCode;
import com.gymflow.plan.PlanRepository;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import com.gymflow.workout.WorkoutLogRepository;
import com.gymflow.workout.WorkoutPlanRepository;
import com.gymflow.workout.WorkoutService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Trozo 9f: en modo demo, el único OWNER real sale de DEMO_OWNER_EMAIL/DEMO_OWNER_PASSWORD
 * (nunca aparece en los botones de acceso). Test unitario con mocks (sin Spring ni
 * Testcontainers): solo la validación, antes de tocar nada más.
 */
class DevDataSeederDemoValidationTest {

    private final TenantRepository tenants = mock(TenantRepository.class);
    private final com.gymflow.auth.UserRepository users = mock(com.gymflow.auth.UserRepository.class);
    private final PlanRepository plans = mock(PlanRepository.class);
    private final com.gymflow.plan.MembershipRepository memberships = mock(com.gymflow.plan.MembershipRepository.class);
    private final ClassSessionRepository classSessions = mock(ClassSessionRepository.class);
    private final BookingService bookingService = mock(BookingService.class);
    private final WorkoutPlanRepository workoutPlans = mock(WorkoutPlanRepository.class);
    private final WorkoutLogRepository workoutLogs = mock(WorkoutLogRepository.class);
    private final WorkoutService workoutService = mock(WorkoutService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final Environment environment = mock(Environment.class);

    private DevDataSeeder seeder(String ownerEmail, String ownerPassword) {
        Tenant tenant = new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG);
        when(tenants.findBySlug(Tenant.DEFAULT_SLUG)).thenReturn(Optional.of(tenant));
        when(plans.findByTenantIdAndCode(tenant.getId(), PlanCode.BASICA))
            .thenReturn(Optional.of(mock(Plan.class)));
        when(plans.findByTenantIdAndCode(tenant.getId(), PlanCode.COMPLETA))
            .thenReturn(Optional.of(mock(Plan.class)));
        when(plans.findByTenantIdAndCode(tenant.getId(), PlanCode.ATLETA_PRO))
            .thenReturn(Optional.of(mock(Plan.class)));
        when(environment.matchesProfiles("demo")).thenReturn(true);

        return new DevDataSeeder(
            tenants, users, plans, memberships, classSessions, bookingService, workoutPlans, workoutLogs,
            workoutService, passwordEncoder, environment, ownerEmail, ownerPassword);
    }

    @Test
    void failsClearlyWhenTheOwnerEmailIsMissing() {
        DevDataSeeder seeder = seeder("", "una-contraseña-de-doce-caracteres");

        assertThrows(IllegalStateException.class, seeder::run);
        verify(users, never()).save(any());
    }

    @Test
    void failsClearlyWhenTheOwnerPasswordIsMissing() {
        DevDataSeeder seeder = seeder("propietario@olympusgym.test", "");

        assertThrows(IllegalStateException.class, seeder::run);
        verify(users, never()).save(any());
    }

    @Test
    void rejectsAnOwnerPasswordShorterThan12Characters() {
        DevDataSeeder seeder = seeder("propietario@olympusgym.test", "corta123456");

        assertThrows(IllegalStateException.class, seeder::run);
        verify(users, never()).save(any());
    }
}
