"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import * as api from "@/lib/api";
import { ApiError, type AdminBooking } from "@/lib/api";

const MADRID_TZ = "Europe/Madrid";

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("es-ES", {
    weekday: "short",
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
    timeZone: MADRID_TZ,
  });
}

const statusLabels: Record<string, string> = {
  BOOKED: "Reservada",
  WAITLIST: "En espera",
  CANCELLED: "Cancelada",
  ATTENDED: "Asistió",
  NO_SHOW: "No se presentó",
};

export default function AdminReservasPage() {
  const { token } = useAuth();
  const [bookings, setBookings] = useState<AdminBooking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancellingId, setCancellingId] = useState<string | null>(null);

  async function load() {
    if (!token) return;
    setLoading(true);
    try {
      const data = await api.fetchAdminBookings(token);
      setBookings(data);
    } catch {
      setError("No se pudieron cargar las reservas.");
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
    setCancellingId(bookingId);
    setError(null);
    try {
      await api.cancelAdminBooking(token, bookingId);
      await load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "No se pudo cancelar la reserva.");
    } finally {
      setCancellingId(null);
    }
  }

  const activeBookings = bookings.filter((b) => b.status === "BOOKED" || b.status === "WAITLIST");

  return (
    <main className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
      <Badge tone="neutral">Datos de ejemplo · demo</Badge>
      <h1 className="mt-4 font-display text-4xl font-extrabold uppercase leading-none tracking-tight">Reservas</h1>
      <p className="mt-2 text-text-muted">
        Como personal del gimnasio puedes cancelar la reserva de cualquier socio, sin el límite de 2 horas.
      </p>

      {error && (
        <p role="alert" className="mt-6 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
          {error}
        </p>
      )}

      <div className="mt-6 flex flex-col gap-3">
        {loading && <p className="text-sm text-text-muted">Cargando reservas...</p>}
        {!loading && activeBookings.length === 0 && (
          <p className="text-sm text-text-muted">No hay reservas activas en los próximos 30 días.</p>
        )}
        {activeBookings.map((booking) => (
          <Card key={booking.bookingId} className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <Badge tone={booking.status === "WAITLIST" ? "warning" : "accent"}>
                {statusLabels[booking.status] ?? booking.status}
                {booking.waitlistPosition ? ` (#${booking.waitlistPosition})` : ""}
              </Badge>
              <p className="mt-1 font-medium text-text">{booking.memberName}</p>
              <p className="text-sm text-text-muted">
                {booking.sessionName} · {formatDateTime(booking.sessionStartsAt)}
              </p>
            </div>
            <Button
              variant="destructive"
              size="sm"
              disabled={cancellingId === booking.bookingId}
              onClick={() => handleCancel(booking.bookingId)}
            >
              Cancelar
            </Button>
          </Card>
        ))}
      </div>
    </main>
  );
}
