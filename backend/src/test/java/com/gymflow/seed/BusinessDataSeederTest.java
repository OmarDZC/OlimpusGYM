package com.gymflow.seed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanCode;
import com.gymflow.plan.PlanRepository;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Trozo 8a-2: datos de negocio reales (tenant + 3 tarifas). Test unitario con mocks (sin
 * Spring ni Testcontainers): {@code BusinessDataSeederProductionProfileTest} ya prueba el
 * resultado de verdad contra una base de datos real con el perfil "prod".
 */
class BusinessDataSeederTest {

    private final TenantRepository tenants = mock(TenantRepository.class);
    private final PlanRepository plans = mock(PlanRepository.class);

    @Test
    void createsTheTenantAndTheThreePlansWhenNoneExistYet() {
        when(tenants.findBySlug(Tenant.DEFAULT_SLUG)).thenReturn(Optional.empty());
        Tenant savedTenant = new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG);
        when(tenants.save(any())).thenReturn(savedTenant);
        when(plans.findByTenantIdAndCode(eq(savedTenant.getId()), any())).thenReturn(Optional.empty());
        when(plans.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new BusinessDataSeeder(tenants, plans).run();

        verify(tenants).save(any());
        ArgumentCaptor<Plan> captor = ArgumentCaptor.forClass(Plan.class);
        verify(plans, times(3)).save(captor.capture());
        assertEquals(
            java.util.Set.of(PlanCode.BASICA, PlanCode.COMPLETA, PlanCode.ATLETA_PRO),
            captor.getAllValues().stream().map(Plan::getCode).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void doesNothingWhenTheTenantAndThePlansAlreadyExist() {
        Tenant existingTenant = new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG);
        when(tenants.findBySlug(Tenant.DEFAULT_SLUG)).thenReturn(Optional.of(existingTenant));
        when(plans.findByTenantIdAndCode(eq(existingTenant.getId()), any()))
            .thenReturn(Optional.of(new Plan(
                existingTenant.getId(), PlanCode.BASICA, "Tarifa Básica", 3900, 4, 7, false, false)));

        new BusinessDataSeeder(tenants, plans).run();

        verify(tenants, never()).save(any());
        verify(plans, never()).save(any());
    }
}
