import type { Metadata } from "next";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Footer } from "@/components/layout/Footer";
import { TarifaCta } from "@/components/public/TarifaCta";
import { fetchPublicPlans, type PublicPlan } from "@/lib/api-server";

export const metadata: Metadata = {
  title: "Tarifas | OlympusGYM",
  description: "Las tres tarifas de OlympusGYM: Básica, Completa y Atleta Pro, con precio y lo que incluye cada una. Gimnasio de demostración, pago de demostración.",
  openGraph: {
    title: "Tarifas | OlympusGYM",
    description: "Básica, Completa y Atleta Pro: compara precio y beneficios de cada tarifa.",
  },
};

function formatPrice(cents: number): string {
  return (cents / 100).toLocaleString("es-ES", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function benefitBullets(plan: PublicPlan): string[] {
  const bullets: string[] = [
    plan.classesPerMonth === null ? "Reservas ilimitadas." : `Hasta ${plan.classesPerMonth} reservas al mes.`,
  ];
  if (plan.aiTrainer && plan.aiAssistant) {
    bullets.push("Entrenador con inteligencia artificial y asistente de reservas con inteligencia artificial.");
  } else if (plan.aiTrainer) {
    bullets.push("Entrenador con inteligencia artificial.");
  } else if (plan.aiAssistant) {
    bullets.push("Asistente de reservas con inteligencia artificial.");
  } else {
    bullets.push("Sin entrenador con inteligencia artificial ni asistente de reservas.");
  }
  if (plan.planCode === "ATLETA_PRO") {
    bullets.push("Ajuste semanal automático de tu rutina.");
  }
  return bullets;
}

export default async function TarifasPage() {
  let plans: PublicPlan[] = [];
  let loadError: string | null = null;
  try {
    plans = await fetchPublicPlans();
  } catch {
    loadError = "No se pudieron cargar las tarifas. Inténtalo de nuevo en unos minutos.";
  }

  return (
    <main>
      <section className="mx-auto max-w-5xl px-4 py-16 sm:px-6">
        <Badge tone="neutral">Datos reales del gimnasio · pago de demostración</Badge>
        <h1 className="mt-4 font-display text-5xl font-extrabold uppercase leading-none tracking-tight sm:text-6xl">
          Tarifas <span className="text-accent-text">sin permanencia</span>
        </h1>
        <p className="mt-4 max-w-2xl text-base text-text-muted">
          OlympusGYM es un gimnasio de demostración: el pago siempre es de prueba, nunca se cobra
          nada de verdad. Elige la tarifa que mejor se ajuste a tu entrenamiento.
        </p>

        {loadError && (
          <p role="alert" className="mt-8 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
            {loadError}
          </p>
        )}

        {plans.length > 0 && (
          <>
            <div className="mt-10 grid grid-cols-1 gap-6 sm:grid-cols-3">
              {plans.map((plan) => (
                <Card key={plan.planCode} className="flex flex-col">
                  <p className="font-display text-2xl font-bold uppercase tracking-tight">{plan.planName}</p>
                  <p className="mt-2 font-display text-4xl font-bold text-accent-text tabular-nums">
                    {formatPrice(plan.priceCents)} € <span className="text-base font-sans text-text-muted">/mes</span>
                  </p>
                  <ul className="mt-4 flex flex-1 flex-col gap-1.5 text-sm text-text-muted">
                    {benefitBullets(plan).map((bullet) => (
                      <li key={bullet}>{bullet}</li>
                    ))}
                  </ul>
                  <TarifaCta className="mt-6 w-full" />
                </Card>
              ))}
            </div>

            <h2 className="mt-16 font-display text-2xl font-bold uppercase tracking-tight">Comparación</h2>
            <div className="mt-4 overflow-x-auto rounded-card border border-border">
              <table className="w-full min-w-[480px] border-collapse text-left text-sm">
                <caption className="sr-only">Comparación de beneficios entre las tres tarifas</caption>
                <thead>
                  <tr className="border-b border-border bg-surface-2">
                    <th scope="col" className="px-4 py-3 font-semibold text-text">
                      Tarifa
                    </th>
                    {plans.map((plan) => (
                      <th key={plan.planCode} scope="col" className="px-4 py-3 font-semibold text-text">
                        {plan.planName}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  <tr className="border-b border-border">
                    <th scope="row" className="px-4 py-3 font-medium text-text-muted">
                      Precio
                    </th>
                    {plans.map((plan) => (
                      <td key={plan.planCode} className="whitespace-nowrap px-4 py-3 tabular-nums text-text">
                        {formatPrice(plan.priceCents)} €/mes
                      </td>
                    ))}
                  </tr>
                  <tr className="border-b border-border">
                    <th scope="row" className="px-4 py-3 font-medium text-text-muted">
                      Reservas
                    </th>
                    {plans.map((plan) => (
                      <td key={plan.planCode} className="px-4 py-3 text-text">
                        {plan.classesPerMonth === null ? "Ilimitadas" : `Hasta ${plan.classesPerMonth} al mes`}
                      </td>
                    ))}
                  </tr>
                  <tr className="border-b border-border">
                    <th scope="row" className="px-4 py-3 font-medium text-text-muted">
                      Entrenador con IA
                    </th>
                    {plans.map((plan) => (
                      <td key={plan.planCode} className="px-4 py-3 text-text">
                        {plan.aiTrainer ? "Sí" : "No"}
                      </td>
                    ))}
                  </tr>
                  <tr className="border-b border-border">
                    <th scope="row" className="px-4 py-3 font-medium text-text-muted">
                      Asistente de reservas con IA
                    </th>
                    {plans.map((plan) => (
                      <td key={plan.planCode} className="px-4 py-3 text-text">
                        {plan.aiAssistant ? "Sí" : "No"}
                      </td>
                    ))}
                  </tr>
                  <tr>
                    <th scope="row" className="px-4 py-3 font-medium text-text-muted">
                      Ajuste semanal automático
                    </th>
                    {plans.map((plan) => (
                      <td key={plan.planCode} className="px-4 py-3 text-text">
                        {plan.planCode === "ATLETA_PRO" ? "Sí" : "No"}
                      </td>
                    ))}
                  </tr>
                </tbody>
              </table>
            </div>
          </>
        )}
      </section>

      <Footer />
    </main>
  );
}
