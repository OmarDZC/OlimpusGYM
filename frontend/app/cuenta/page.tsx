"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { MembershipSummaryCard } from "@/components/account/MembershipSummaryCard";
import { NextClassCard } from "@/components/account/NextClassCard";
import { StreakCard } from "@/components/account/StreakCard";
import { TodayWorkoutCard } from "@/components/account/TodayWorkoutCard";
import { Badge } from "@/components/ui/Badge";
import * as api from "@/lib/api";

export default function CuentaInicioPage() {
  const { user, token } = useAuth();
  const [planName, setPlanName] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    api
      .fetchMembership(token)
      .then((m) => setPlanName(m.planName))
      .catch(() => setPlanName(null));
  }, [token]);

  if (!user) return null;

  const firstName = user.fullName.split(" ")[0];

  return (
    <main className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <Badge tone="neutral">Datos de ejemplo · demo</Badge>
      <div className="mt-4 flex flex-wrap items-center gap-3">
        <h1 className="font-display text-5xl font-extrabold uppercase leading-none tracking-tight">
          Hola, {firstName}
        </h1>
        {planName && <Badge tone="accent">Socio {planName.replace(/^Tarifa\s+/i, "")}</Badge>}
      </div>
      <p className="mt-2 text-text-muted">Socio de OlympusGYM. Esto es lo que te toca hoy.</p>

      <div className="mt-6 flex flex-col gap-6">
        <NextClassCard />
        <StreakCard />
        <TodayWorkoutCard />
        <MembershipSummaryCard />
      </div>
    </main>
  );
}
