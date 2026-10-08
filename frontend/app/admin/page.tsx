"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import * as api from "@/lib/api";
import type { AdminSummary } from "@/lib/api";

function formatPrice(cents: number): string {
  return (cents / 100).toLocaleString("es-ES", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function KpiCard({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <Card>
      <p className="text-xs font-semibold uppercase tracking-wide text-text-muted">{label}</p>
      <p className="mt-2 font-display text-3xl font-bold tabular-nums tracking-tight">{value}</p>
      {hint && <p className="mt-1 text-xs text-text-dim">{hint}</p>}
    </Card>
  );
}

export default function AdminResumenPage() {
  const { token } = useAuth();
  const [summary, setSummary] = useState<AdminSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    api
      .fetchAdminSummary(token)
      .then(setSummary)
      .catch(() => setError("No se pudo cargar el resumen."))
      .finally(() => setLoading(false));
  }, [token]);

  return (
    <main className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
      <Badge tone="neutral">Datos de ejemplo · demo</Badge>
      <h1 className="mt-4 font-display text-4xl font-extrabold uppercase leading-none tracking-tight">
        Resumen general
      </h1>

      {loading && <p className="mt-6 text-sm text-text-muted">Cargando...</p>}
      {error && (
        <p role="alert" className="mt-6 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
          {error}
        </p>
      )}

      {summary && (
        <>
          <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
            <KpiCard label="Socios activos" value={String(summary.activeMembers)} />
            <KpiCard label="Clases de hoy" value={String(summary.todaySessions)} />
            <KpiCard
              label="Ocupación de hoy"
              value={`${summary.todayBookedSeats}/${summary.todayCapacity}`}
              hint="plazas reservadas"
            />
            {summary.monthlyRevenueCents !== null && (
              <KpiCard label="Ingresos del mes" value={`${formatPrice(summary.monthlyRevenueCents)} €`} hint="calculado desde las cuotas activas" />
            )}
          </div>

          <section className="mt-8">
            <h2 className="font-display text-2xl font-bold uppercase tracking-tight">Socios que conviene contactar</h2>
            <p className="mt-1 text-sm text-text-muted">Sin una reserva en los últimos 14 días.</p>

            <div className="mt-4 flex flex-col gap-3">
              {summary.membersToContact.length === 0 && (
                <Card>
                  <p className="text-sm text-text-muted">Todos los socios activos han venido hace poco.</p>
                </Card>
              )}
              {summary.membersToContact.map((member) => (
                <Card key={member.userId} className="flex flex-wrap items-center justify-between gap-4">
                  <div>
                    <p className="font-medium text-text">{member.fullName}</p>
                    <p className="text-sm text-text-muted">
                      {member.lastBookingAt
                        ? `Última reserva: ${new Date(member.lastBookingAt).toLocaleDateString("es-ES")}`
                        : "Nunca ha reservado una clase"}
                    </p>
                  </div>
                  <a
                    href={`mailto:${member.email}`}
                    className="rounded-control border border-border bg-surface px-3.5 py-2 text-sm font-medium text-text hover:bg-surface-2"
                  >
                    Contactar
                  </a>
                </Card>
              ))}
            </div>
          </section>
        </>
      )}
    </main>
  );
}
