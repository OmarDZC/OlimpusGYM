"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import * as api from "@/lib/api";
import { ApiError, type MyBooking } from "@/lib/api";

const MADRID_TZ = "Europe/Madrid";

function formatDateTime(iso: string): string {
  const date = new Date(iso);
  const day = date.toLocaleDateString("es-ES", {
    weekday: "short",
    day: "2-digit",
    month: "short",
    timeZone: MADRID_TZ,
  });
  const time = date.toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit", timeZone: MADRID_TZ });
  return `${day} · ${time}`;
}

/** Próxima clase reservada, con enlace para cancelar (referencia v2/mi-cuenta). */
export function NextClassCard() {
  const { token } = useAuth();
  const [bookings, setBookings] = useState<MyBooking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState(false);

  async function load() {
    if (!token) return;
    try {
      const data = await api.fetchMyBookings(token);
      setBookings(data.filter((b) => b.status === "BOOKED" || b.status === "WAITLIST"));
    } catch {
      setError("No se pudo cargar tu próxima clase.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  async function handleCancel(bookingId: string) {
    if (!token) return;
    setCancelling(true);
    setError(null);
    try {
      await api.cancelBooking(token, bookingId);
      await load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "No se pudo cancelar la reserva.");
    } finally {
      setCancelling(false);
    }
  }

  if (loading) {
    return (
      <Card>
        <p className="text-sm text-text-muted">Cargando tu próxima clase...</p>
      </Card>
    );
  }

  const next = bookings[0];

  if (!next) {
    return (
      <Card>
        <p className="text-xs font-semibold uppercase tracking-wide text-text-muted">Tu próxima clase</p>
        <p className="mt-2 text-sm text-text-muted">
          Todavía no tienes ninguna reserva.{" "}
          <Link href="/horario" className="text-accent-text hover:underline">
            Reserva tu primera clase
          </Link>
          .
        </p>
      </Card>
    );
  }

  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <Badge tone={next.status === "WAITLIST" ? "warning" : "accent"}>
            {next.status === "WAITLIST" ? `En lista de espera (#${next.waitlistPosition})` : "Tu próxima clase reservada"}
          </Badge>
          <p className="mt-2 font-display text-2xl font-bold uppercase tracking-tight">{next.sessionName}</p>
          <p className="text-sm text-text-muted">
            {formatDateTime(next.startsAt)} · {next.room}
            {next.coachName ? ` · ${next.coachName}` : ""}
          </p>
        </div>
        <Button variant="destructive" size="sm" disabled={cancelling} onClick={() => handleCancel(next.id)}>
          Cancelar reserva
        </Button>
      </div>
      {error && (
        <p
          role="alert"
          className="mt-3 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger"
        >
          {error}
        </p>
      )}
    </Card>
  );
}
