"use client";

import { Check } from "lucide-react";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import * as api from "@/lib/api";
import type { TodayWorkout } from "@/lib/api";
import { cn } from "@/lib/cn";

/** Rutina de hoy con casillas por serie (referencia v2/mi-cuenta). Marca series hechas de verdad. */
export function TodayWorkoutCard() {
  const { token } = useAuth();
  const [workout, setWorkout] = useState<TodayWorkout | null>(null);
  const [loading, setLoading] = useState(true);
  const [togglingKey, setTogglingKey] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    if (!token) return;
    try {
      const data = await api.fetchTodayWorkout(token);
      setWorkout(data);
    } catch {
      setError("No se pudo cargar tu rutina de hoy.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  async function toggleSet(exerciseIndex: number, setIndex: number, currentlyDone: boolean) {
    if (!token || !workout) return;
    const key = `${exerciseIndex}:${setIndex}`;
    setTogglingKey(key);
    setError(null);
    try {
      await api.logWorkoutSet(token, {
        dayIndex: workout.dayIndex,
        exerciseIndex,
        setIndex,
        done: !currentlyDone,
      });
      await load();
    } catch {
      setError("No se pudo guardar el cambio. Inténtalo de nuevo.");
    } finally {
      setTogglingKey(null);
    }
  }

  if (loading) {
    return (
      <Card>
        <p className="text-sm text-text-muted">Cargando tu rutina de hoy...</p>
      </Card>
    );
  }

  if (!workout || !workout.hasPlan) {
    return (
      <Card>
        <p className="text-xs font-semibold uppercase tracking-wide text-text-muted">Entrenamiento de hoy</p>
        <p className="mt-2 text-sm text-text-muted">
          Todavía no tienes una rutina asignada. El entrenador con inteligencia artificial que genera tu plan
          semanal llega en una fase posterior del proyecto.
        </p>
      </Card>
    );
  }

  if (workout.restDay) {
    return (
      <Card>
        <p className="text-xs font-semibold uppercase tracking-wide text-text-muted">{workout.dayLabel}</p>
        <p className="mt-2 font-display text-2xl font-bold uppercase tracking-tight">Hoy toca descanso</p>
        <p className="mt-1 text-sm text-text-muted">Aprovecha para recuperar: mañana sigue la rutina.</p>
      </Card>
    );
  }

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <Badge tone="accent">Rutina de hoy</Badge>
          <p className="mt-2 font-display text-2xl font-bold uppercase tracking-tight">{workout.dayLabel}</p>
          <p className="text-sm text-text-muted">{workout.focus}</p>
        </div>
      </div>

      {error && (
        <p role="alert" className="mt-3 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
          {error}
        </p>
      )}

      <div className="mt-4 flex flex-col divide-y divide-border">
        {workout.exercises.map((exercise, index) => (
          <div key={exercise.exerciseIndex} className="flex flex-wrap items-center justify-between gap-3 py-3">
            <div>
              <p className="font-display text-lg font-bold uppercase tracking-tight">
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
                const key = `${exercise.exerciseIndex}:${set.setIndex}`;
                return (
                  <button
                    key={set.setIndex}
                    type="button"
                    disabled={togglingKey === key}
                    onClick={() => toggleSet(exercise.exerciseIndex, set.setIndex, set.done)}
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
    </Card>
  );
}
