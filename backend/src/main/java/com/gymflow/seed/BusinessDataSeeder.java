package com.gymflow.seed;

import com.gymflow.plan.Plan;
import com.gymflow.plan.PlanCode;
import com.gymflow.plan.PlanRepository;
import com.gymflow.tenant.Tenant;
import com.gymflow.tenant.TenantRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos de negocio reales (Trozo 8a-2): el tenant único "olympusgym" y las 3 tarifas reales de
 * LOGIC.md §3, con sus precios y límites. Corre en **todos** los perfiles, incluida
 * producción: sin tarifas, nadie podría registrarse ni el panel de gestión tendría nada que
 * mostrar, ni siquiera en un despliegue real. Esto es justo lo contrario de
 * {@link DevDataSeeder} (socios, clases y reservas de *ejemplo*, solo dev/demo): estos datos no
 * son de muestra, son la configuración real del gimnasio.
 *
 * <p>Idempotente (no duplica nada si ya existe) y va primero (@Order): tanto
 * {@link DevDataSeeder} como {@link ProductionAdminSeeder} dan por hecho que el tenant ya
 * existe cuando les toca correr.
 */
@Component
@Order(0)
public class BusinessDataSeeder implements CommandLineRunner {

    private final TenantRepository tenants;
    private final PlanRepository plans;

    public BusinessDataSeeder(TenantRepository tenants, PlanRepository plans) {
        this.tenants = tenants;
        this.plans = plans;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Tenant tenant = tenants.findBySlug(Tenant.DEFAULT_SLUG)
            .orElseGet(() -> tenants.save(new Tenant("OlympusGYM", Tenant.DEFAULT_SLUG)));

        ensurePlan(tenant, PlanCode.BASICA, "Tarifa Básica", 3900, 4, 7, false, false);
        ensurePlan(tenant, PlanCode.COMPLETA, "Tarifa Completa", 5900, null, 14, true, true);
        ensurePlan(tenant, PlanCode.ATLETA_PRO, "Tarifa Atleta Pro", 7900, null, 14, true, true);
    }

    private void ensurePlan(
            Tenant tenant,
            PlanCode code,
            String name,
            int priceCents,
            Integer classesPerMonth,
            int bookingWindowDays,
            boolean aiTrainer,
            boolean aiAssistant) {
        plans.findByTenantIdAndCode(tenant.getId(), code)
            .orElseGet(() -> plans.save(new Plan(
                tenant.getId(), code, name, priceCents, classesPerMonth, bookingWindowDays, aiTrainer, aiAssistant)));
    }
}
