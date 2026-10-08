"use client";

import { Check } from "lucide-react";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { Card } from "@/components/ui/Card";
import * as api from "@/lib/api";
import type { Streak } from "@/lib/api";
import { cn } from "@/lib/cn";

/** Constancia semanal: casillas Lun-Dom + barra de progreso (referencia v2/mi-cuenta). */
export function StreakCard() {
  const { token } = useAuth();
  const [streak, setStreak] = useState<Streak | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!token) return;
    api
      .fetchStreak(token)
      .then(setStreak)
      .catch(() => setStreak(null))
      .finally(() => setLoading(false));
  }, [token]);

  if (loading) {
    return (
      <Card>
        <p className="text-sm text-text-muted">Cargando tu constancia...</p>
      </Card>
    );
  }

  if (!streak || !streak.hasPlan) {
    return null;
  }

  const progressPct = streak.targetDays > 0 ? Math.round((streak.completedDays / streak.targetDays) * 100) : 0;

  return (
    <Card>
      <div className="flex items-center justify-between">
        <p className="text-xs font-semibold uppercase tracking-wide text-text-muted">Tu constancia esta semana</p>
      </div>

      <div className="mt-3 grid grid-cols-7 gap-2">
        {streak.days.map((day) => (
          <div key={day.dayIndex} className="flex flex-col items-center gap-1.5">
            <span className="text-xs font-semibold uppercase text-text-dim">{day.label}</span>
            <div
              className={cn(
                "flex h-9 w-9 items-center justify-center rounded-control border text-xs",
                day.restDay
                  ? "border-border bg-surface-2 text-text-dim"
                  : day.trained
                    ? "border-accent bg-accent text-on-accent"
                    : day.isToday
                      ? "border-accent/60 bg-surface-2 text-accent-text"
                      : "border-border bg-surface-2 text-text-dim",
              )}
            >
              {day.trained ? <Check size={16} /> : day.restDay ? "–" : ""}
            </div>
          </div>
        ))}
      </div>

      <div className="mt-4">
        <div className="h-2 overflow-hidden rounded-full bg-surface-2">
          <div className="h-full bg-accent transition-all" style={{ width: `${progressPct}%` }} />
        </div>
        <p className="mt-2 text-sm text-text-muted">
          <span className="font-semibold text-text">
            {streak.completedDays} de {streak.targetDays}
          </span>{" "}
          entrenamientos · vas al {progressPct}% del objetivo semanal
        </p>
      </div>
    </Card>
  );
}
