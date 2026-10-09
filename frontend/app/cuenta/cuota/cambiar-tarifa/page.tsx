"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import * as api from "@/lib/api";
import { ApiError, type Membership, type PlanChangeOption } from "@/lib/api";

const MADRID_TZ = "Europe/Madrid";

function formatPrice(cents: number): string {
  return (cents / 100).toLocaleString("es-ES", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("es-ES", { day: "2-digit", month: "long", year: "numeric", timeZone: MADRID_TZ });
}

/**
 * Cambio de tarifa (Trozo 9b, LOGIC.md §12): subir siempre al instante con el importe
 * prorrateado ya calculado por el backend, bajar siempre programado a la siguiente
 * renovación. Toda la lógica de precios vive en el servidor: aquí solo se muestra y se
 * confirma lo que la API ya ha calculado.
 */
export default function CambiarTarifaPage() {
  const { token } = useAuth();
  const router = useRouter();

  const [membership, setMembership] = useState<Membership | null>(null);
  const [options, setOptions] = useState<PlanChangeOption[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [selected, setSelected] = useState<PlanChangeOption | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [undoing, setUndoing] = useState(false);

  async function loadData() {
    if (!token) return;
    try {
      const [membershipResult, optionsResult] = await Promise.all([
        api.fetchMembership(token),
        api.fetchPlanOptions(token),
      ]);
      setMembership(membershipResult);
      setOptions(optionsResult);
    } catch {
      setLoadError("No se pudieron cargar las tarifas disponibles.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  async function handleUndoDowngrade() {
    if (!token || undoing) return;
    setUndoing(true);
    setActionError(null);
    try {
      await api.cancelScheduledDowngrade(token);
      await loadData();
    } catch (e) {
      setActionError(e instanceof ApiError ? e.message : "No se pudo deshacer el cambio. Inténtalo de nuevo.");
    } finally {
      setUndoing(false);
    }
  }

  async function handleConfirm() {
    if (!token || !selected || submitting) return;
    setSubmitting(true);
    setActionError(null);
    try {
      if (selected.direction === "UPGRADE") {
        await api.upgradeMembershipPlan(token, selected.planCode);
      } else {
        await api.scheduleMembershipDowngrade(token, selected.planCode);
      }
      router.push("/cuenta/cuota");
    } catch (e) {
      setActionError(e instanceof ApiError ? e.message : "No se pudo completar el cambio. Inténtalo de nuevo.");
      setSubmitting(false);
    }
  }

  return (
    <main className="mx-auto max-w-2xl px-4 py-10 sm:px-6">
      <h1 className="font-display text-4xl font-extrabold uppercase leading-none tracking-tight">Cambiar de tarifa</h1>
      <p className="mt-2 text-text-muted">
        Subir de tarifa se aplica al momento: solo pagas la diferencia de lo que queda de este periodo. Bajar de
        tarifa se aplica en tu próxima renovación, sin perder lo que ya tienes hasta entonces.
      </p>

      {loading && <p className="mt-6 text-sm text-text-muted">Cargando...</p>}
      {loadError && (
        <p role="alert" className="mt-6 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
          {loadError}
        </p>
      )}

      {membership?.pendingPlanName && (
        <Card className="mt-6 border-warning/40 bg-warning/5">
          <p className="text-sm text-text">
            Tienes programado un cambio a <span className="font-semibold">{membership.pendingPlanName}</span> para el{" "}
            {formatDate(membership.periodEnd)}.
          </p>
          <Button type="button" variant="secondary" size="sm" disabled={undoing} onClick={handleUndoDowngrade} className="mt-3">
            {undoing ? "Deshaciendo..." : "Deshacer"}
          </Button>
        </Card>
      )}

      {options && (
        <div className="mt-6 flex flex-col gap-4">
          {options.map((option) => (
            <Card
              key={option.planCode}
              data-testid={`plan-option-${option.planCode}`}
              className={option.direction === "CURRENT" ? "border-accent/40" : undefined}
            >
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <p className="font-display text-2xl font-bold uppercase tracking-tight">{option.planName}</p>
                  <p className="mt-1 font-display text-2xl font-bold text-accent-text tabular-nums">
                    {formatPrice(option.priceCents)} € <span className="text-sm font-sans text-text-muted">/mes</span>
                  </p>
                </div>
                {option.direction === "CURRENT" && <Badge tone="accent">Tu tarifa actual</Badge>}
              </div>

              {option.direction === "UPGRADE" && option.amountDueTodayCents !== null && (
                <p className="mt-3 text-sm text-text-muted">
                  Hoy pagarías <span className="whitespace-nowrap font-medium text-text">{formatPrice(option.amountDueTodayCents)} €</span>{" "}
                  (la diferencia por lo que queda de tu periodo actual). Desde tu próxima renovación, el precio completo.
                </p>
              )}
              {option.direction === "DOWNGRADE" && option.effectiveAt && (
                <p className="mt-3 text-sm text-text-muted">
                  Se aplicaría en tu próxima renovación, el {formatDate(option.effectiveAt)}. Hasta entonces conservas tu
                  tarifa actual.
                </p>
              )}

              {option.direction !== "CURRENT" && (
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  className="mt-4"
                  onClick={() => {
                    setSelected(option);
                    setActionError(null);
                  }}
                >
                  {option.direction === "UPGRADE" ? "Subir ahora" : "Cambiar en la próxima renovación"}
                </Button>
              )}
            </Card>
          ))}
        </div>
      )}

      {selected && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 sm:items-center" onClick={() => setSelected(null)}>
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Confirmar cambio de tarifa"
            onClick={(event) => event.stopPropagation()}
            className="w-full max-w-md rounded-card border border-border bg-surface p-6"
          >
            <h2 className="font-display text-xl font-bold uppercase tracking-tight">Confirma el cambio</h2>
            {selected.direction === "UPGRADE" ? (
              <p className="mt-3 text-sm text-text-muted">
                Vas a subir a <span className="font-semibold text-text">{selected.planName}</span>. Hoy pagarías{" "}
                <span className="whitespace-nowrap font-medium text-text">
                  {formatPrice(selected.amountDueTodayCents ?? 0)} €
                </span>{" "}
                y el cambio se aplica al momento.
              </p>
            ) : (
              <p className="mt-3 text-sm text-text-muted">
                Vas a cambiar a <span className="font-semibold text-text">{selected.planName}</span> para el{" "}
                {selected.effectiveAt ? formatDate(selected.effectiveAt) : "tu próxima renovación"}. Hasta entonces
                conservas tu tarifa actual.
              </p>
            )}
            <p className="mt-3 text-sm text-text-dim">Pago de demostración: no se cobra nada de verdad.</p>

            {actionError && (
              <p role="alert" className="mt-3 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
                {actionError}
              </p>
            )}

            <div className="mt-5 flex gap-2">
              <Button type="button" variant="primary" disabled={submitting} onClick={handleConfirm}>
                {submitting ? "Aplicando..." : "Confirmar"}
              </Button>
              <Button type="button" variant="secondary" disabled={submitting} onClick={() => setSelected(null)}>
                Cancelar
              </Button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
