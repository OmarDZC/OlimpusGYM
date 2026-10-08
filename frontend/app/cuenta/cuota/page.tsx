"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import * as api from "@/lib/api";
import type { Membership } from "@/lib/api";

const MADRID_TZ = "Europe/Madrid";

function formatPrice(cents: number): string {
  return (cents / 100).toLocaleString("es-ES", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("es-ES", { day: "2-digit", month: "long", year: "numeric", timeZone: MADRID_TZ });
}

const statusLabels: Record<Membership["status"], string> = {
  ACTIVE: "Activa",
  PAUSED: "Pausada",
  CANCELLED: "Cancelada",
};

export default function MiCuotaPage() {
  const { token } = useAuth();
  const [membership, setMembership] = useState<Membership | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    api
      .fetchMembership(token)
      .then(setMembership)
      .catch(() => setError("No se pudo cargar tu cuota."))
      .finally(() => setLoading(false));
  }, [token]);

  return (
    <main className="mx-auto max-w-2xl px-4 py-10 sm:px-6">
      <h1 className="font-display text-4xl font-extrabold uppercase leading-none tracking-tight">Mi cuota</h1>
      <p className="mt-2 text-text-muted">El pago real con pasarela es una fase opcional posterior del proyecto.</p>

      {loading && <p className="mt-6 text-sm text-text-muted">Cargando...</p>}
      {error && (
        <p role="alert" className="mt-6 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
          {error}
        </p>
      )}

      {membership && (
        <div className="mt-6 flex flex-col gap-4">
          <Card>
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-text-muted">Tarifa actual</p>
                <p className="mt-1 font-display text-3xl font-bold uppercase tracking-tight">{membership.planName}</p>
              </div>
              <Badge tone={membership.status === "ACTIVE" ? "accent" : "warning"}>
                {statusLabels[membership.status]}
              </Badge>
            </div>
            <p className="mt-4 font-display text-4xl font-bold text-accent-text tabular-nums">
              {formatPrice(membership.priceCents)} € <span className="text-base font-sans text-text-muted">/mes</span>
            </p>
          </Card>

          <Card>
            <p className="text-xs font-semibold uppercase tracking-wide text-text-muted">Incluye</p>
            <ul className="mt-2 flex flex-col gap-1.5 text-sm text-text-muted">
              <li>Sala de pesas de 06:00 a 23:00, taquillas y duchas.</li>
              <li>Clases dirigidas según tu tarifa.</li>
              {membership.aiTrainer && <li>Entrenador con inteligencia artificial.</li>}
              {membership.aiAssistant && <li>Asistente de reservas con inteligencia artificial.</li>}
            </ul>
          </Card>

          <Card className="flex flex-wrap items-center justify-between gap-4 text-sm">
            <div>
              <p className="text-text-muted">Alta</p>
              <p className="font-medium text-text">{formatDate(membership.startedAt)}</p>
            </div>
            <div>
              <p className="text-text-muted">Próxima renovación</p>
              <p className="font-medium text-text">
                {membership.renewsAt ? formatDate(membership.renewsAt) : "Sin fecha fijada (demo)"}
              </p>
            </div>
          </Card>
        </div>
      )}
    </main>
  );
}
