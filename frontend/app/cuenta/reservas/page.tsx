"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ReservarTabs } from "@/components/account/ReservarTabs";
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

export default function MisReservasPage() {
  const { token } = useAuth();
  const [bookings, setBookings] = useState<MyBooking[]>([]);
  const [history, setHistory] = useState<MyBooking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancellingId, setCancellingId] = useState<string | null>(null);

  async function loadBookings() {
    if (!token) return;
    setLoading(true);
    try {
      const [active, past] = await Promise.all([api.fetchMyBookings(token), api.fetchMyBookingsHistory(token)]);
      setBookings(active);
      setHistory(past);
    } catch {
      setError("No se pudieron cargar tus reservas.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadBookings();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  async function handleCancel(booking: MyBooking) {
    if (!token) return;
    setCancellingId(booking.id);
    setError(null);
    try {
      await api.cancelBooking(token, booking.id);
      await loadBookings();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "No se pudo cancelar la reserva.");
    } finally {
      setCancellingId(null);
    }
  }

  return (
    <main className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <h1 className="font-display text-4xl font-extrabold uppercase leading-none tracking-tight">Mis reservas</h1>
      <p className="mt-2 text-text-muted">Puedes cancelar hasta 2 horas antes de que empiece la clase.</p>

      <div className="mt-6">
        <ReservarTabs />
      </div>

      {error && (
        <p
          role="alert"
          className="mt-4 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger"
        >
          {error}
        </p>
      )}

      <div className="mt-6 flex flex-col gap-3">
        {loading && <p className="text-sm text-text-muted">Cargando tus reservas...</p>}
        {!loading && bookings.length === 0 && (
          <Card>
            <p className="text-sm text-text-muted">
              Todavía no tienes ninguna reserva próxima.{" "}
              <Link href="/horario" className="text-accent-text hover:underline">
                Reserva tu primera clase
              </Link>
              .
            </p>
          </Card>
        )}
        {bookings.map((booking) => (
          <Card key={booking.id} className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <Badge tone={booking.status === "WAITLIST" ? "warning" : "accent"}>
                {booking.status === "WAITLIST" ? `En espera (#${booking.waitlistPosition})` : "Reservado"}
              </Badge>
              <p className="mt-1 font-display text-lg font-bold uppercase tracking-tight">{booking.sessionName}</p>
              <p className="text-sm text-text-muted">
                {formatDateTime(booking.startsAt)} · {booking.room}
                {booking.coachName ? ` · ${booking.coachName}` : ""}
              </p>
            </div>
            <Button
              variant="destructive"
              size="sm"
              disabled={cancellingId === booking.id}
              onClick={() => handleCancel(booking)}
            >
              Cancelar
            </Button>
          </Card>
        ))}
      </div>

      {!loading && history.length > 0 && (
        <div className="mt-10">
          <h2 className="font-display text-2xl font-bold uppercase tracking-tight">Historial</h2>
          <p className="mt-1 text-sm text-text-muted">Clases que ya han pasado.</p>
          <div className="mt-4 flex flex-col gap-3">
            {history.map((booking) => (
              <Card key={booking.id} className="flex flex-wrap items-center justify-between gap-4 opacity-80">
                <div>
                  <Badge tone="neutral">{booking.status === "WAITLIST" ? "Estuviste en espera" : "Reservada"}</Badge>
                  <p className="mt-1 font-display text-lg font-bold uppercase tracking-tight">{booking.sessionName}</p>
                  <p className="text-sm text-text-muted">
                    {formatDateTime(booking.startsAt)} · {booking.room}
                    {booking.coachName ? ` · ${booking.coachName}` : ""}
                  </p>
                </div>
              </Card>
            ))}
          </div>
        </div>
      )}
    </main>
  );
}
