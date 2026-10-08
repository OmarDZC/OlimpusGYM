"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Select } from "@/components/ui/Select";
import * as api from "@/lib/api";
import { ApiError, type MyBooking, type ScheduleSession } from "@/lib/api";
import { cn } from "@/lib/cn";

const DISCIPLINES = ["CrossFit", "Fuerza", "Yoga", "Boxeo", "Spinning", "Halterofilia"];
const DAYS_AHEAD = 14;
const MADRID_TZ = "Europe/Madrid";

function startOfDayUtc(date: Date): Date {
  return new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
}

export default function HorarioPage() {
  const { status, user, token } = useAuth();
  const router = useRouter();

  const days = useMemo(() => {
    const today = startOfDayUtc(new Date());
    return Array.from({ length: DAYS_AHEAD }, (_, i) => new Date(today.getTime() + i * 86_400_000));
  }, []);

  const [selectedDayIndex, setSelectedDayIndex] = useState(0);
  const [discipline, setDiscipline] = useState("");
  const [sessions, setSessions] = useState<ScheduleSession[]>([]);
  const [myBookings, setMyBookings] = useState<MyBooking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actingSessionId, setActingSessionId] = useState<string | null>(null);

  const selectedDay = days[selectedDayIndex];
  const canSeeOwnBookings = status === "signed-in" && user?.role === "MEMBER";

  async function loadSchedule() {
    setLoading(true);
    setError(null);
    try {
      const from = selectedDay.toISOString();
      const to = new Date(selectedDay.getTime() + 86_400_000).toISOString();
      const data = await api.fetchSchedule({ from, to, discipline: discipline || undefined });
      setSessions(data);
    } catch {
      setError("No se pudo cargar el horario. Inténtalo de nuevo.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadSchedule();
    // selectedDay cambia junto con selectedDayIndex; no hace falta añadirlo aparte.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedDayIndex, discipline]);

  useEffect(() => {
    if (canSeeOwnBookings && token) {
      api.fetchMyBookings(token).then(setMyBookings).catch(() => setMyBookings([]));
    } else {
      setMyBookings([]);
    }
  }, [canSeeOwnBookings, token]);

  function bookingFor(sessionId: string): MyBooking | undefined {
    return myBookings.find(
      (b) => b.sessionId === sessionId && (b.status === "BOOKED" || b.status === "WAITLIST"),
    );
  }

  async function afterAction() {
    await loadSchedule();
    if (token) {
      api.fetchMyBookings(token).then(setMyBookings).catch(() => {});
    }
  }

  async function handleBook(session: ScheduleSession) {
    if (status !== "signed-in" || !token) {
      router.push("/acceso");
      return;
    }
    setActingSessionId(session.id);
    setError(null);
    try {
      await api.bookSession(token, session.id);
      await afterAction();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "No se pudo reservar. Inténtalo de nuevo.");
    } finally {
      setActingSessionId(null);
    }
  }

  async function handleCancel(booking: MyBooking) {
    if (!token) return;
    setActingSessionId(booking.sessionId);
    setError(null);
    try {
      await api.cancelBooking(token, booking.id);
      await afterAction();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "No se pudo cancelar. Inténtalo de nuevo.");
    } finally {
      setActingSessionId(null);
    }
  }

  return (
    <main className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
      <Badge tone="neutral">Datos de ejemplo · demo</Badge>
      <h1 className="mt-4 font-display text-5xl font-extrabold uppercase leading-none tracking-tight sm:text-6xl">
        Horario de <span className="text-accent-text">clases</span>
      </h1>
      <p className="mt-2 max-w-xl text-text-muted">
        Consulta las sesiones dirigidas y reserva tu plaza. Puedes cancelar hasta 2 horas antes
        de que empiece la clase.
      </p>

      <div className="mt-8 max-w-xs">
        <label htmlFor="discipline-filter" className="sr-only">
          Tipo de clase
        </label>
        <Select id="discipline-filter" value={discipline} onChange={(e) => setDiscipline(e.target.value)}>
          <option value="">Todas las clases</option>
          {DISCIPLINES.map((d) => (
            <option key={d} value={d}>
              {d}
            </option>
          ))}
        </Select>
      </div>

      <div className="mt-4 flex gap-2 overflow-x-auto pb-2">
        {days.map((day, index) => {
          const isActive = index === selectedDayIndex;
          return (
            <button
              key={day.toISOString()}
              type="button"
              data-testid={`schedule-day-${day.toISOString().slice(0, 10)}`}
              onClick={() => setSelectedDayIndex(index)}
              className={cn(
                "flex min-w-[64px] flex-col items-center rounded-control border px-3 py-2 text-sm transition-colors",
                isActive
                  ? "border-accent bg-accent text-on-accent"
                  : "border-border bg-surface text-text-muted hover:text-text",
              )}
            >
              <span className="text-xs uppercase">
                {day.toLocaleDateString("es-ES", { weekday: "short", timeZone: MADRID_TZ }).replace(".", "")}
              </span>
              <span className="font-display text-lg font-bold">
                {day.toLocaleDateString("es-ES", { day: "2-digit", timeZone: MADRID_TZ })}
              </span>
            </button>
          );
        })}
      </div>

      {error && (
        <p
          role="alert"
          className="mt-6 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger"
        >
          {error}
        </p>
      )}

      <div className="mt-6 flex flex-col gap-3">
        {loading && <p className="text-sm text-text-muted">Cargando horario...</p>}
        {!loading && sessions.length === 0 && (
          <p className="text-sm text-text-muted">No hay clases programadas este día.</p>
        )}
        {sessions.map((session) => {
          const myBooking = canSeeOwnBookings ? bookingFor(session.id) : undefined;
          const isActing = actingSessionId === session.id;
          const isFull = session.freeSlots <= 0;

          return (
            <Card
              key={session.id}
              data-testid={`session-${session.id}`}
              className="flex flex-wrap items-center justify-between gap-4"
            >
              <div className="flex items-center gap-4">
                <div className="flex flex-col items-center rounded-control border border-border bg-surface-2 px-3 py-2">
                  <span className="font-mono text-lg font-semibold tabular-nums">
                    {new Date(session.startsAt).toLocaleTimeString("es-ES", {
                      hour: "2-digit",
                      minute: "2-digit",
                      timeZone: MADRID_TZ,
                    })}
                  </span>
                </div>
                <div>
                  <Badge tone="neutral">{session.discipline}</Badge>
                  <p className="mt-1 font-display text-xl font-bold uppercase tracking-tight">{session.name}</p>
                  <p className="text-sm text-text-muted">
                    {session.room}
                    {session.coachName ? ` · ${session.coachName}` : ""}
                  </p>
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-3">
                {isFull ? (
                  <Badge tone="danger">
                    Completa{session.waitlistCount > 0 ? ` · ${session.waitlistCount} en espera` : ""}
                  </Badge>
                ) : (
                  <span className="text-sm text-text-muted">{session.freeSlots} plazas libres</span>
                )}

                {myBooking ? (
                  <div className="flex items-center gap-2">
                    <Badge tone={myBooking.status === "WAITLIST" ? "warning" : "accent"}>
                      {myBooking.status === "WAITLIST"
                        ? `En espera (#${myBooking.waitlistPosition})`
                        : "Reservado"}
                    </Badge>
                    <Button variant="destructive" size="sm" disabled={isActing} onClick={() => handleCancel(myBooking)}>
                      Cancelar
                    </Button>
                  </div>
                ) : (
                  <Button variant="primary" size="sm" disabled={isActing} onClick={() => handleBook(session)}>
                    {isFull ? "Unirme a la lista" : "Reservar"}
                  </Button>
                )}
              </div>
            </Card>
          );
        })}
      </div>
    </main>
  );
}
