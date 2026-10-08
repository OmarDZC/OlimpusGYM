"use client";

import { Check } from "lucide-react";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import * as api from "@/lib/api";
import { ApiError, type GeneratedWeeklyPlan, type TrainerGoal, type TrainerLevel } from "@/lib/api";
import { cn } from "@/lib/cn";

const GOALS: { value: TrainerGoal; label: string }[] = [
  { value: "strength", label: "Fuerza máxima" },
  { value: "hypertrophy", label: "Hipertrofia muscular" },
  { value: "fat_loss", label: "Pérdida de grasa" },
  { value: "general", label: "Salud y resistencia" },
];

const LEVELS: { value: TrainerLevel; label: string }[] = [
  { value: "beginner", label: "Principiante" },
  { value: "intermediate", label: "Intermedio" },
  { value: "advanced", label: "Avanzado" },
];

const FREQUENCIES = [3, 4, 5];

const EQUIPMENT_OPTIONS = ["Gimnasio completo", "Mancuernas y barra", "Peso corporal"];

/** Botón de una sola elección (objetivo, nivel, frecuencia): mismo lenguaje visual que los días de /horario. */
function ChoiceButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={cn(
        "rounded-control border px-3 py-2.5 text-left text-sm font-medium transition-colors",
        active ? "border-accent bg-accent text-on-accent" : "border-border bg-surface text-text-muted hover:text-text",
      )}
    >
      {children}
    </button>
  );
}

export default function EntrenadorIaPage() {
  const { status, token } = useAuth();
  const router = useRouter();

  const [goal, setGoal] = useState<TrainerGoal>("strength");
  const [level, setLevel] = useState<TrainerLevel>("intermediate");
  const [daysPerWeek, setDaysPerWeek] = useState(4);
  const [equipment, setEquipment] = useState(EQUIPMENT_OPTIONS[0]);
  const [limitations, setLimitations] = useState("");

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [plan, setPlan] = useState<GeneratedWeeklyPlan | null>(null);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (status !== "signed-in" || !token) {
      router.push("/acceso");
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const result = await api.generateWorkoutPlan(token, {
        goal,
        level,
        daysPerWeek,
        equipment: [equipment],
        limitations: limitations
          .split(",")
          .map((l) => l.trim())
          .filter(Boolean),
      });
      setPlan(result);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "No se pudo generar tu rutina. Inténtalo de nuevo.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
      <Badge tone="neutral">Planificación con IA</Badge>
      <h1 className="mt-4 font-display text-5xl font-extrabold uppercase leading-none tracking-tight sm:text-6xl">
        Tu entrenador, <span className="text-accent-text">siempre contigo</span>
      </h1>
      <p className="mt-2 max-w-xl text-text-muted">
        Cuéntanos tu objetivo, tu nivel y los días que puedes entrenar: generamos tu rutina semanal al momento.
        Incluido en las tarifas Completa y Atleta Pro.
      </p>

      <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-2 lg:items-start">
        <Card>
          <div className="flex items-center justify-between">
            <h2 className="font-display text-xl font-bold uppercase tracking-tight">Configura tu sesión</h2>
            {status === "signed-in" && <Badge tone="accent">Listo para calcular</Badge>}
          </div>

          <form onSubmit={handleSubmit} className="mt-4 flex flex-col gap-5">
            <div>
              <p className="text-sm font-medium text-text">Objetivo prioritario</p>
              <div className="mt-2 grid grid-cols-2 gap-2">
                {GOALS.map((g) => (
                  <ChoiceButton key={g.value} active={goal === g.value} onClick={() => setGoal(g.value)}>
                    {g.label}
                  </ChoiceButton>
                ))}
              </div>
            </div>

            <div>
              <p className="text-sm font-medium text-text">Nivel actual</p>
              <div className="mt-2 grid grid-cols-3 gap-2">
                {LEVELS.map((l) => (
                  <ChoiceButton key={l.value} active={level === l.value} onClick={() => setLevel(l.value)}>
                    {l.label}
                  </ChoiceButton>
                ))}
              </div>
            </div>

            <div>
              <p className="text-sm font-medium text-text">Frecuencia semanal</p>
              <div className="mt-2 grid grid-cols-3 gap-2">
                {FREQUENCIES.map((f) => (
                  <ChoiceButton key={f} active={daysPerWeek === f} onClick={() => setDaysPerWeek(f)}>
                    {f} días
                  </ChoiceButton>
                ))}
              </div>
            </div>

            <div>
              <p className="text-sm font-medium text-text">Material disponible</p>
              <div className="mt-2 flex flex-col gap-2">
                {EQUIPMENT_OPTIONS.map((option) => {
                  const active = equipment === option;
                  return (
                    <button
                      key={option}
                      type="button"
                      onClick={() => setEquipment(option)}
                      aria-pressed={active}
                      className={cn(
                        "flex items-center justify-between rounded-control border px-3.5 py-2.5 text-left text-sm font-medium transition-colors",
                        active
                          ? "border-accent bg-accent/10 text-text"
                          : "border-border bg-surface text-text-muted hover:text-text",
                      )}
                    >
                      {option}
                      {active && <Check size={16} className="text-accent-text" aria-hidden="true" />}
                    </button>
                  );
                })}
              </div>
            </div>

            <Field label="Lesiones o limitaciones (opcional)" htmlFor="limitations">
              <Input
                id="limitations"
                placeholder="Ej: molestia en el hombro derecho"
                value={limitations}
                onChange={(e) => setLimitations(e.target.value)}
              />
            </Field>

            {error && (
              <p role="alert" className="rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
                {error}
              </p>
            )}

            <Button type="submit" variant="primary" disabled={submitting} className="w-full">
              {submitting ? "Creando tu rutina..." : "Crear mi rutina"}
            </Button>
          </form>
        </Card>

        <Card>
          {!plan && !submitting && (
            <p className="text-sm text-text-muted">
              Tu rutina aparecerá aquí en cuanto la generes. Puedes volver a crearla cuando quieras: la nueva
              sustituye a la de esta semana.
            </p>
          )}
          {submitting && <p className="text-sm text-text-muted">Generando tu rutina... puede tardar unos segundos.</p>}

          {plan && (
            <div className="flex flex-col gap-6">
              <div className="flex items-center justify-between">
                <h2 className="font-display text-xl font-bold uppercase tracking-tight">Rutina semanal adaptada</h2>
                <Badge tone="accent">Personalizada</Badge>
              </div>

              {plan.days.map((day) => (
                <div key={day.dayIndex}>
                  <p className="text-xs font-semibold uppercase tracking-wide text-text-muted">
                    {day.label} · {day.focus}
                  </p>
                  <div className="mt-2 flex flex-col divide-y divide-border">
                    {day.exercises.map((exercise, index) => (
                      <div key={exercise.exerciseIndex} className="flex flex-wrap items-center justify-between gap-3 py-2.5">
                        <div>
                          <p className="font-medium text-text">
                            {index + 1}. {exercise.name}
                          </p>
                          {exercise.note && <p className="text-xs text-text-dim">{exercise.note}</p>}
                        </div>
                        <p className="text-sm text-text-muted">
                          {exercise.sets} series × {exercise.reps}
                          {exercise.weightKg ? ` · ${exercise.weightKg} kg` : ""}
                          {exercise.restSeconds ? ` · descanso ${exercise.restSeconds} seg` : ""}
                        </p>
                      </div>
                    ))}
                  </div>
                </div>
              ))}

              <p className="text-sm text-text-muted">
                Ya puedes verla y marcar tus series desde{" "}
                <a href="/cuenta" className="text-accent-text hover:underline">
                  Mi cuenta
                </a>
                .
              </p>
            </div>
          )}
        </Card>
      </div>
    </main>
  );
}
