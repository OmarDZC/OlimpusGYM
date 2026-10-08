"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import * as api from "@/lib/api";
import { ApiError, type ScheduleSession } from "@/lib/api";

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

const emptyForm = {
  name: "",
  discipline: "",
  room: "",
  startsAt: "",
  endsAt: "",
  capacity: "16",
  repeatWeeks: "1",
};

export default function AdminClasesPage() {
  const { token } = useAuth();
  const [sessions, setSessions] = useState<ScheduleSession[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [cancellingId, setCancellingId] = useState<string | null>(null);
  const [form, setForm] = useState(emptyForm);

  async function load() {
    if (!token) return;
    setLoading(true);
    try {
      const data = await api.fetchAdminSessions(token);
      setSessions(data);
    } catch {
      setError("No se pudieron cargar las clases.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  async function handleCreate(event: React.FormEvent) {
    event.preventDefault();
    if (!token) return;
    setFormError(null);
    setSubmitting(true);
    try {
      await api.createAdminSession(token, {
        name: form.name,
        discipline: form.discipline,
        room: form.room,
        startsAt: new Date(form.startsAt).toISOString(),
        endsAt: new Date(form.endsAt).toISOString(),
        capacity: Number(form.capacity),
        repeatWeeks: Number(form.repeatWeeks) || 1,
      });
      setForm(emptyForm);
      await load();
    } catch (e) {
      setFormError(e instanceof ApiError ? e.message : "No se pudo crear la clase.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCancel(sessionId: string) {
    if (!token) return;
    setCancellingId(sessionId);
    setError(null);
    try {
      await api.cancelAdminSession(token, sessionId);
      await load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "No se pudo cancelar la clase.");
    } finally {
      setCancellingId(null);
    }
  }

  return (
    <main className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
      <Badge tone="neutral">Datos de ejemplo · demo</Badge>
      <h1 className="mt-4 font-display text-4xl font-extrabold uppercase leading-none tracking-tight">Clases</h1>
      <p className="mt-2 text-text-muted">
        Crear una clase nueva, repetirla varias semanas seguidas, o cancelarla (se avisa a los socios con reserva).
      </p>

      <Card className="mt-6">
        <h2 className="font-display text-xl font-bold uppercase tracking-tight">Crear clase</h2>
        <form onSubmit={handleCreate} className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Field label="Nombre" htmlFor="name">
            <Input id="name" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </Field>
          <Field label="Disciplina" htmlFor="discipline">
            <Input
              id="discipline"
              required
              value={form.discipline}
              onChange={(e) => setForm({ ...form, discipline: e.target.value })}
            />
          </Field>
          <Field label="Sala" htmlFor="room">
            <Input id="room" required value={form.room} onChange={(e) => setForm({ ...form, room: e.target.value })} />
          </Field>
          <Field label="Empieza" htmlFor="startsAt">
            <Input
              id="startsAt"
              type="datetime-local"
              required
              value={form.startsAt}
              onChange={(e) => setForm({ ...form, startsAt: e.target.value })}
            />
          </Field>
          <Field label="Termina" htmlFor="endsAt">
            <Input
              id="endsAt"
              type="datetime-local"
              required
              value={form.endsAt}
              onChange={(e) => setForm({ ...form, endsAt: e.target.value })}
            />
          </Field>
          <Field label="Aforo" htmlFor="capacity">
            <Input
              id="capacity"
              type="number"
              min={1}
              required
              value={form.capacity}
              onChange={(e) => setForm({ ...form, capacity: e.target.value })}
            />
          </Field>
          <Field label="Repetir semanas seguidas" htmlFor="repeatWeeks">
            <Input
              id="repeatWeeks"
              type="number"
              min={1}
              max={12}
              value={form.repeatWeeks}
              onChange={(e) => setForm({ ...form, repeatWeeks: e.target.value })}
            />
          </Field>

          <div className="flex items-end sm:col-span-2 lg:col-span-3">
            <Button type="submit" variant="primary" disabled={submitting}>
              {submitting ? "Creando..." : "Crear clase"}
            </Button>
          </div>

          {formError && (
            <p role="alert" className="sm:col-span-2 lg:col-span-3 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
              {formError}
            </p>
          )}
        </form>
      </Card>

      {error && (
        <p role="alert" className="mt-6 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
          {error}
        </p>
      )}

      <div className="mt-6 flex flex-col gap-3">
        {loading && <p className="text-sm text-text-muted">Cargando clases...</p>}
        {!loading && sessions.length === 0 && <p className="text-sm text-text-muted">No hay clases programadas.</p>}
        {sessions.map((session) => (
          <Card
            key={session.id}
            data-testid={`admin-session-${session.id}`}
            className="flex flex-wrap items-center justify-between gap-4"
          >
            <div>
              <Badge tone="neutral">{session.discipline}</Badge>
              <p className="mt-1 font-display text-lg font-bold uppercase tracking-tight">{session.name}</p>
              <p className="text-sm text-text-muted">
                {formatDateTime(session.startsAt)} · {session.room}
                {session.coachName ? ` · ${session.coachName}` : ""} · {session.bookedCount}/{session.capacity} plazas
                {session.waitlistCount > 0 ? ` · ${session.waitlistCount} en espera` : ""}
              </p>
            </div>
            <Button
              variant="destructive"
              size="sm"
              disabled={cancellingId === session.id}
              onClick={() => handleCancel(session.id)}
            >
              Cancelar clase
            </Button>
          </Card>
        ))}
      </div>
    </main>
  );
}
