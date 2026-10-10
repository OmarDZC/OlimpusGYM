"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import * as api from "@/lib/api";
import { ApiError, type AdminContactMessage } from "@/lib/api";

const MADRID_TZ = "Europe/Madrid";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString("es-ES", {
    day: "2-digit",
    month: "long",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    timeZone: MADRID_TZ,
  });
}

export default function AdminMensajesPage() {
  const { token } = useAuth();
  const [messages, setMessages] = useState<AdminContactMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [markingId, setMarkingId] = useState<string | null>(null);

  async function load() {
    if (!token) return;
    setLoading(true);
    try {
      const data = await api.fetchAdminMessages(token);
      setMessages(data);
    } catch {
      setError("No se pudo cargar la lista de mensajes.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  async function handleMarkRead(messageId: string) {
    if (!token) return;
    setMarkingId(messageId);
    setError(null);
    try {
      const updated = await api.markAdminMessageRead(token, messageId);
      setMessages((prev) => prev.map((m) => (m.id === messageId ? updated : m)));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "No se pudo marcar como leído.");
    } finally {
      setMarkingId(null);
    }
  }

  return (
    <main className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <h1 className="font-display text-4xl font-extrabold uppercase leading-none tracking-tight">Mensajes</h1>
      <p className="mt-2 text-text-muted">Lo que escriben desde el formulario de contacto, el más reciente primero.</p>

      {error && (
        <p role="alert" className="mt-6 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
          {error}
        </p>
      )}

      <div className="mt-6 flex flex-col gap-3">
        {loading && <p className="text-sm text-text-muted">Cargando mensajes...</p>}
        {!loading && messages.length === 0 && <p className="text-sm text-text-muted">Todavía no hay mensajes.</p>}
        {messages.map((message) => (
          <Card key={message.id} className={message.read ? undefined : "border-accent/40"}>
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <div className="flex items-center gap-2">
                  <p className="font-medium text-text">{message.name}</p>
                  {!message.read && <Badge tone="accent">Sin leer</Badge>}
                </div>
                <p className="text-sm text-text-muted">{message.email}</p>
              </div>
              <p className="text-xs text-text-dim">{formatDate(message.createdAt)}</p>
            </div>

            <p className="mt-3 text-sm text-text">{message.message}</p>

            {!message.read && (
              <Button
                type="button"
                variant="secondary"
                size="sm"
                disabled={markingId === message.id}
                onClick={() => handleMarkRead(message.id)}
                className="mt-4"
              >
                {markingId === message.id ? "Marcando..." : "Marcar como leído"}
              </Button>
            )}
          </Card>
        ))}
      </div>
    </main>
  );
}
