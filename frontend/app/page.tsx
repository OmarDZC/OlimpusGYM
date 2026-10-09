import type { Metadata } from "next";
import { Dumbbell, CalendarCheck, Sparkles, MessageCircle } from "lucide-react";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Footer } from "@/components/layout/Footer";
import { fetchPublicPlans, fetchTodaySchedule, type PublicPlan } from "@/lib/api-server";
import type { ScheduleSession } from "@/lib/api";

const MADRID_TZ = "Europe/Madrid";

export const metadata: Metadata = {
  title: "OlympusGYM | Gimnasio de demostración",
  description: "Reservas de clases con aforo, entrenador con inteligencia artificial y asistente de reservas. Gimnasio de demostración, datos de ejemplo.",
  openGraph: {
    title: "OlympusGYM",
    description: "Forja tu propio Olimpo: clases con aforo real, entrenador con IA y asistente de reservas.",
  },
};

function formatPrice(cents: number): string {
  return (cents / 100).toLocaleString("es-ES", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

const offerings = [
  { icon: Dumbbell, title: "Sala de pesas", description: "Abierta de 06:00 a 23:00, todos los días." },
  { icon: CalendarCheck, title: "Clases dirigidas", description: "Horario real con aforo y lista de espera." },
  { icon: Sparkles, title: "Entrenador con IA", description: "Tu rutina semanal, adaptada a tu nivel." },
  { icon: MessageCircle, title: "Asistente de reservas", description: "Reserva o cancela una clase hablando con él." },
];

const testimonials = [
  { name: "Javier Montero", quote: "Llevo seis meses entrenando aquí y la rutina que me genera el entrenador con IA de verdad se adapta a mi progreso." },
  { name: "Lucía Serrano", quote: "Me apunto a las clases desde el móvil en un minuto y siempre sé cuántas plazas quedan." },
  { name: "Raúl Alarcón", quote: "El aforo real de cada clase me ayuda a planificar la semana sin sorpresas al llegar." },
];

export default async function HomePage() {
  const [plans, todaySchedule] = await Promise.all([
    fetchPublicPlans().catch((): PublicPlan[] => []),
    fetchTodaySchedule().catch((): ScheduleSession[] => []),
  ]);

  return (
    <main>
      <section className="mx-auto flex max-w-5xl flex-col items-center gap-6 px-4 py-20 text-center sm:px-6">
        <Badge tone="neutral">Datos de ejemplo · demo</Badge>

        <h1 className="font-display text-5xl font-extrabold uppercase leading-none tracking-tight sm:text-7xl">
          Forja tu propio <span className="text-accent-text">Olimpo</span>
        </h1>

        <p className="max-w-xl text-base text-text-muted">
          Entrena como los de siempre. Reserva como ahora: clases con aforo real, entrenador con
          inteligencia artificial y un asistente que reserva por ti.
        </p>

        <div className="flex flex-wrap items-center justify-center gap-3">
          <Button href="/horario" variant="primary">
            Ver horario
          </Button>
          <Button href="/tarifas" variant="secondary">
            Ver tarifas
          </Button>
        </div>
      </section>

      <section className="border-y border-border bg-surface">
        <div className="mx-auto grid max-w-5xl grid-cols-1 gap-6 px-4 py-14 sm:grid-cols-2 sm:px-6 lg:grid-cols-4">
          {offerings.map((item) => (
            <div key={item.title} className="flex flex-col items-start gap-2">
              <item.icon size={28} className="text-accent-text" aria-hidden="true" />
              <p className="font-display text-lg font-bold uppercase tracking-tight">{item.title}</p>
              <p className="text-sm text-text-muted">{item.description}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-5xl px-4 py-16 sm:px-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <h2 className="font-display text-3xl font-bold uppercase tracking-tight">Clases de hoy</h2>
          <Button href="/horario" variant="secondary" size="sm">
            Ver horario completo
          </Button>
        </div>

        {todaySchedule.length === 0 ? (
          <p className="mt-6 text-sm text-text-muted">No quedan clases por empezar hoy. Consulta el horario completo.</p>
        ) : (
          <div className="mt-6 flex flex-col gap-3">
            {todaySchedule.slice(0, 5).map((session) => (
              <Card key={session.id} className="flex flex-wrap items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  <div className="flex flex-col items-center rounded-control border border-border bg-surface-2 px-3 py-2">
                    <span className="font-mono text-lg font-semibold tabular-nums">
                      {new Date(session.startsAt).toLocaleTimeString("es-ES", {
                        hour: "2-digit",
                        minute: "2-digit",
                        timeZone: MADRID_TZ,
                      })}
                    </span>
                  </div>
                  <div>
                    <Badge tone="neutral">{session.discipline}</Badge>
                    <p className="mt-1 font-display text-xl font-bold uppercase tracking-tight">{session.name}</p>
                    <p className="text-sm text-text-muted">
                      {session.room}
                      {session.coachName ? ` · ${session.coachName}` : ""}
                    </p>
                  </div>
                </div>
                {session.freeSlots <= 0 ? (
                  <Badge tone="danger">Completa</Badge>
                ) : (
                  <span className="text-sm text-text-muted">{session.freeSlots} plazas libres</span>
                )}
              </Card>
            ))}
          </div>
        )}
      </section>

      <section className="border-y border-border bg-surface">
        <div className="mx-auto max-w-5xl px-4 py-16 sm:px-6">
          <h2 className="font-display text-3xl font-bold uppercase tracking-tight">Tarifas</h2>
          {plans.length > 0 && (
            <div className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-3">
              {plans.map((plan) => (
                <Card key={plan.planCode}>
                  <p className="font-display text-xl font-bold uppercase tracking-tight">{plan.planName}</p>
                  <p className="mt-2 font-display text-3xl font-bold text-accent-text tabular-nums">
                    {formatPrice(plan.priceCents)} € <span className="text-sm font-sans text-text-muted">/mes</span>
                  </p>
                </Card>
              ))}
            </div>
          )}
          <Button href="/tarifas" variant="secondary" className="mt-6">
            Ver todas las tarifas
          </Button>
        </div>
      </section>

      <section className="mx-auto max-w-5xl px-4 py-16 sm:px-6">
        <h2 className="font-display text-3xl font-bold uppercase tracking-tight">Lo que dicen de nosotros</h2>
        <p className="mt-1 text-xs uppercase tracking-wide text-text-dim">Testimonios de ejemplo</p>
        <div className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-3">
          {testimonials.map((testimonial) => (
            <Card key={testimonial.name}>
              <p className="text-sm text-text-muted">&ldquo;{testimonial.quote}&rdquo;</p>
              <p className="mt-4 text-sm font-semibold text-text">{testimonial.name}</p>
            </Card>
          ))}
        </div>
      </section>

      <section className="border-t border-border bg-surface">
        <div className="mx-auto max-w-5xl px-4 py-16 sm:px-6">
          <h2 className="font-display text-3xl font-bold uppercase tracking-tight">Dónde estamos</h2>
          <div className="mt-4 flex flex-col gap-1 text-sm text-text-muted">
            <p>Calle del Hierro 12, 28045 Madrid (dirección de ejemplo)</p>
            <p>Sala de pesas: 06:00 a 23:00, todos los días</p>
          </div>
        </div>
      </section>

      <Footer />
    </main>
  );
}
