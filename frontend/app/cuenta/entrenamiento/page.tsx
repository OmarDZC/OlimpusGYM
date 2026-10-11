"use client";

import { Check, Dumbbell } from "lucide-react";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import * as api from "@/lib/api";
import type { DayProgress, WeekWorkout } from "@/lib/api";
import { cn } from "@/lib/cn";

/**
 * Rutina semanal vigente completa (Trozo 9d): antes esta pantalla no mostraba nada de la
 * rutina, solo un botón de vuelta a Entrenador IA (fallo real visto probando a mano). Generar
 * y cambiar el objetivo sigue pasando en /entrenador-ia; aquí se ve y se marcan series.
 */
export default function MiEntrenamientoPage() {
  const { token } = useAuth();
  const [week, setWeek] = useState<WeekWorkout | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [togglingKey, setTogglingKey] = useState<string | null>(null);

  async function load() {
    if (!token) return;
    try {
      const data = await api.fetchWeekWorkout(token);
      setWeek(data);
    } catch {
      setError("No se pudo cargar tu rutina.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  async function toggleSet(dayIndex: number, exerciseIndex: number, setIndex: number, currentlyDone: boolean) {
    if (!token) return;
    const key = `${dayIndex}:${exerciseIndex}:${setIndex}`;
    setTogglingKey(key);
    setError(null);
    try {
      await api.logWorkoutSet(token, { dayIndex, exerciseIndex, setIndex, done: !currentlyDone });
      await load();
    } catch {
      setError("No se pudo guardar el cambio. Inténtalo de nuevo.");
    } finally {
      setTogglingKey(null);
    }
  }

  if (loading) {
    return (
      <main className="mx-auto max-w-2xl px-4 py-16 text-center sm:px-6">
        <p className="text-sm text-text-muted">Cargando tu rutina...</p>
      </main>
    );
  }

  if (!week || !week.hasPlan) {
    return (
      <main className="mx-auto max-w-2xl px-4 py-16 text-center sm:px-6">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full border border-border bg-surface">
          <Dumbbell size={24} className="text-accent-text" aria-hidden="true" />
        </div>
        <Badge tone="neutral" className="mt-4">
          Entrenador con IA
        </Badge>
        <h1 className="mt-3 font-display text-4xl font-extrabold uppercase leading-none tracking-tight">
          Todavía no tienes una rutina
        </h1>
        <Card className="mt-6 text-left">
          <p className="text-sm text-text-muted">
            Configura tu objetivo, tu nivel y los días que puedes entrenar en{" "}
            <strong className="text-text">Entrenador IA</strong> y genera tu rutina de esta semana.
          </p>
          <Button href="/entrenador-ia" variant="primary" className="mt-4">
            Ir a Entrenador IA
          </Button>
        </Card>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <Badge tone="accent">Rutina de esta semana</Badge>
          <h1 className="mt-2 font-display text-3xl font-extrabold uppercase leading-none tracking-tight">
            Mi entrenamiento
          </h1>
        </div>
        <Button href="/entrenador-ia" variant="secondary">
          Generar otra rutina
        </Button>
      </div>

      {error && (
        <p role="alert" className="mt-4 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
          {error}
        </p>
      )}

      <div className="mt-6 flex flex-col gap-4">
        {week.days.map((day) => (
          <DayCard
            key={day.dayIndex}
            day={day}
            isToday={day.dayIndex === week.todayIndex}
            togglingKey={togglingKey}
            onToggleSet={toggleSet}
          />
        ))}
      </div>
    </main>
  );
}

function DayCard({
  day,
  isToday,
  togglingKey,
  onToggleSet,
}: {
  day: DayProgress;
  isToday: boolean;
  togglingKey: string | null;
  onToggleSet: (dayIndex: number, exerciseIndex: number, setIndex: number, currentlyDone: boolean) => void;
}) {
  return (
    <Card className={cn(isToday && "border-accent")}>
      <div className="flex flex-wrap items-center gap-2">
        <p className="font-display text-lg font-bold uppercase tracking-tight">{day.label}</p>
        {isToday && <Badge tone="accent">Hoy</Badge>}
      </div>

      {day.restDay ? (
        <p className="mt-1 text-sm text-text-muted">Descanso</p>
      ) : (
        <>
          {day.focus && <p className="text-sm text-text-muted">{day.focus}</p>}
          <div className="mt-3 flex flex-col divide-y divide-border">
            {day.exercises.map((exercise, index) => (
              <div key={exercise.exerciseIndex} className="flex flex-wrap items-center justify-between gap-3 py-3">
                <div>
                  <p className="font-display text-base font-bold uppercase tracking-tight">
                    {index + 1}. {exercise.name}
                  </p>
                  <p className="text-sm text-text-muted">
                    {exercise.sets.length} series × {exercise.reps}
                    {exercise.weightKg ? ` · ${exercise.weightKg} kg` : ""}
                    {exercise.restSeconds ? ` · descanso ${exercise.restSeconds} seg` : ""}
                  </p>
                  {exercise.note && <p className="text-xs text-text-dim">{exercise.note}</p>}
                </div>

                <div className="flex items-center gap-2">
                  {exercise.sets.map((set) => {
                    const key = `${day.dayIndex}:${exercise.exerciseIndex}:${set.setIndex}`;
                    return (
                      <button
                        key={set.setIndex}
                        type="button"
                        disabled={togglingKey === key}
                        onClick={() => onToggleSet(day.dayIndex, exercise.exerciseIndex, set.setIndex, set.done)}
                        aria-pressed={set.done}
                        aria-label={`Serie ${set.setIndex + 1} de ${exercise.name}, ${set.done ? "hecha" : "sin hacer"}`}
                        className={cn(
                          "flex h-8 w-8 items-center justify-center rounded-control border text-xs font-semibold transition-colors",
                          set.done
                            ? "border-accent bg-accent text-on-accent"
                            : "border-border bg-surface-2 text-text-muted hover:text-text",
                        )}
                      >
                        {set.done ? <Check size={16} /> : set.setIndex + 1}
                      </button>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </Card>
  );
}
