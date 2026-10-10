import type { Metadata } from "next";
import Image from "next/image";
import { Dumbbell, CalendarCheck, Sparkles, MessageCircle } from "lucide-react";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Footer } from "@/components/layout/Footer";
import { HeroHeadline } from "@/components/public/HeroHeadline";
import { LiveStrip } from "@/components/public/LiveStrip";
import { Reveal } from "@/components/motion/Reveal";
import { fetchPublicPlans, fetchTodaySchedule, type PublicPlan } from "@/lib/api-server";
import type { ScheduleSession } from "@/lib/api";
import heroImage from "@/public/images/hero-gimnasio.jpg";
import equipmentImage from "@/public/images/equipo-pesas.jpg";
import classImage from "@/public/images/clase-dirigida.jpg";

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

const cardHover = "transition-transform duration-200 motion-safe:hover:-translate-y-1 hover:border-accent/40";

export default async function HomePage() {
  const [plans, todaySchedule] = await Promise.all([
    fetchPublicPlans().catch((): PublicPlan[] => []),
    fetchTodaySchedule().catch((): ScheduleSession[] => []),
  ]);

  return (
    <main>
      <section className="relative isolate overflow-hidden">
        <Image
          src={heroImage}
          alt=""
          fill
          priority
          placeholder="blur"
          sizes="100vw"
          className="-z-10 object-cover grayscale contrast-125"
        />
        <div className="absolute inset-0 -z-10 bg-gradient-to-t from-bg via-bg/85 to-bg/50" />

        <div className="mx-auto flex max-w-5xl flex-col items-center gap-6 px-4 py-24 text-center sm:px-6 sm:py-32">
          <Badge tone="neutral">Datos de ejemplo · demo</Badge>

          <HeroHeadline />

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

          <div className="w-full max-w-xl">
            <LiveStrip />
          </div>
        </div>
      </section>

      <Reveal className="border-y border-border bg-surface">
        <div className="mx-auto grid max-w-5xl grid-cols-1 gap-6 px-4 py-14 sm:grid-cols-2 sm:px-6 lg:grid-cols-4">
          {offerings.map((item) => (
            <div key={item.title} className="group flex flex-col items-start gap-2">
              <item.icon
                size={28}
                className="text-accent-text transition-transform duration-200 motion-safe:group-hover:scale-110"
                aria-hidden="true"
              />
              <p className="font-display text-lg font-bold uppercase tracking-tight">{item.title}</p>
              <p className="text-sm text-text-muted">{item.description}</p>
            </div>
          ))}
        </div>
      </Reveal>

      <Reveal className="mx-auto max-w-5xl px-4 py-16 sm:px-6">
        <div className="grid grid-cols-1 items-center gap-8 lg:grid-cols-2">
          <div className="relative aspect-[4/3] overflow-hidden rounded-card border border-border">
            <Image
              src={equipmentImage}
              alt="Zona de peso libre con mancuernas ordenadas en el gimnasio"
              fill
              sizes="(min-width: 1024px) 50vw, 100vw"
              placeholder="blur"
              className="object-cover grayscale contrast-125"
            />
          </div>
          <div>
            <h2 className="font-display text-3xl font-bold uppercase tracking-tight">Nuestro hierro</h2>
            <p className="mt-3 text-text-muted">
              Peso libre de sobra, zona funcional y máquinas al día. Nada de esperar turno para la
              serie importante.
            </p>
          </div>
        </div>
      </Reveal>

      <Reveal className="mx-auto max-w-5xl px-4 py-16 sm:px-6">
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-[1fr_1.2fr] lg:items-start">
          <div>
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
                  <Card key={session.id} className={`flex flex-wrap items-center justify-between gap-4 ${cardHover}`}>
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
          </div>

          <div className="relative hidden aspect-[4/3] overflow-hidden rounded-card border border-border lg:block">
            <Image
              src={classImage}
              alt="Socias entrenando en una clase dirigida"
              fill
              sizes="40vw"
              placeholder="blur"
              className="object-cover grayscale contrast-125"
            />
          </div>
        </div>
      </Reveal>

      <Reveal className="border-y border-border bg-surface">
        <div className="mx-auto max-w-5xl px-4 py-16 sm:px-6">
          <h2 className="font-display text-3xl font-bold uppercase tracking-tight">Tarifas</h2>
          {plans.length > 0 && (
            <div className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-3">
              {plans.map((plan) => (
                <Card key={plan.planCode} className={cardHover}>
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
      </Reveal>

      <Reveal className="mx-auto max-w-5xl px-4 py-16 sm:px-6">
        <h2 className="font-display text-3xl font-bold uppercase tracking-tight">Lo que dicen de nosotros</h2>
        <p className="mt-1 text-xs uppercase tracking-wide text-text-dim">Testimonios de ejemplo</p>
        <div className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-3">
          {testimonials.map((testimonial) => (
            <Card key={testimonial.name} className={cardHover}>
              <p className="text-sm text-text-muted">&ldquo;{testimonial.quote}&rdquo;</p>
              <p className="mt-4 text-sm font-semibold text-text">{testimonial.name}</p>
            </Card>
          ))}
        </div>
      </Reveal>

      <Reveal className="border-t border-border bg-surface">
        <div className="mx-auto max-w-5xl px-4 py-16 sm:px-6">
          <h2 className="font-display text-3xl font-bold uppercase tracking-tight">Dónde estamos</h2>
          <div className="mt-4 flex flex-col gap-1 text-sm text-text-muted">
            <p>Calle del Hierro 12, 28045 Madrid (dirección de ejemplo)</p>
            <p>Sala de pesas: 06:00 a 23:00, todos los días</p>
          </div>
        </div>
      </Reveal>

      <Footer />
    </main>
  );
}
