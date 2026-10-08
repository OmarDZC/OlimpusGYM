"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Select } from "@/components/ui/Select";
import * as api from "@/lib/api";
import { ApiError, type AdminMember, type PlanCode } from "@/lib/api";
import { planOptions } from "@/lib/roles";

function formatPrice(cents: number): string {
  return (cents / 100).toLocaleString("es-ES", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export default function AdminSociosPage() {
  const { token, user } = useAuth();
  const isOwner = user?.role === "OWNER";
  const [members, setMembers] = useState<AdminMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [savingId, setSavingId] = useState<string | null>(null);
  const [pendingPlan, setPendingPlan] = useState<Record<string, PlanCode>>({});

  async function load() {
    if (!token) return;
    setLoading(true);
    try {
      const data = await api.fetchAdminMembers(token);
      setMembers(data);
    } catch {
      setError("No se pudo cargar el listado de socios.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  async function handleChangePlan(memberId: string) {
    if (!token) return;
    const newPlan = pendingPlan[memberId];
    if (!newPlan) return;
    setSavingId(memberId);
    setError(null);
    try {
      const updated = await api.changeMemberPlan(token, memberId, newPlan);
      setMembers((prev) => prev.map((m) => (m.userId === memberId ? updated : m)));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "No se pudo cambiar la tarifa.");
    } finally {
      setSavingId(null);
    }
  }

  return (
    <main className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
      <Badge tone="neutral">Datos de ejemplo · demo</Badge>
      <h1 className="mt-4 font-display text-4xl font-extrabold uppercase leading-none tracking-tight">Socios</h1>
      <p className="mt-2 text-text-muted">
        {isOwner
          ? "Como dueño, puedes ver el precio de cada tarifa y cambiarla."
          : "El precio de cada tarifa solo lo ve el dueño."}
      </p>

      {error && (
        <p role="alert" className="mt-6 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
          {error}
        </p>
      )}

      <div className="mt-6 flex flex-col gap-3">
        {loading && <p className="text-sm text-text-muted">Cargando socios...</p>}
        {!loading && members.length === 0 && <p className="text-sm text-text-muted">Todavía no hay socios.</p>}
        {members.map((member) => (
          <Card key={member.userId} className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="font-medium text-text">{member.fullName}</p>
              <p className="text-sm text-text-muted">{member.email}</p>
            </div>

            <div className="flex items-center gap-3">
              <Badge tone="accent">{member.planName ?? "Sin tarifa"}</Badge>
              {isOwner && member.priceCents !== null && (
                <span className="text-sm tabular-nums text-text-muted">{formatPrice(member.priceCents)} €/mes</span>
              )}

              {isOwner && member.planCode && (
                <div className="flex items-center gap-2">
                  <Select
                    aria-label={`Cambiar tarifa de ${member.fullName}`}
                    value={pendingPlan[member.userId] ?? member.planCode}
                    onChange={(e) =>
                      setPendingPlan((prev) => ({ ...prev, [member.userId]: e.target.value as PlanCode }))
                    }
                    className="w-auto"
                  >
                    {planOptions.map((plan) => (
                      <option key={plan.code} value={plan.code}>
                        {plan.label}
                      </option>
                    ))}
                  </Select>
                  <Button
                    variant="secondary"
                    size="sm"
                    disabled={
                      savingId === member.userId ||
                      !pendingPlan[member.userId] ||
                      pendingPlan[member.userId] === member.planCode
                    }
                    onClick={() => handleChangePlan(member.userId)}
                  >
                    Guardar
                  </Button>
                </div>
              )}
            </div>
          </Card>
        ))}
      </div>
    </main>
  );
}
