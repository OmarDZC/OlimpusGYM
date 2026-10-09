"use client";

import { Send, X } from "lucide-react";
import Link from "next/link";
import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import * as api from "@/lib/api";
import { ApiError, type AssistantChatMessage, type AssistantPendingAction } from "@/lib/api";
import { cn } from "@/lib/cn";

const MAX_HISTORY = 20;

type DisplayMessage = { id: string; role: "user" | "assistant"; content: string };

function trimHistory(messages: AssistantChatMessage[]): AssistantChatMessage[] {
  return messages.length > MAX_HISTORY ? messages.slice(messages.length - MAX_HISTORY) : messages;
}

/**
 * Chat del asistente de reservas (Trozo 6b), como panel lateral sobre /horario. Diálogo
 * accesible: foco gestionado (entra en el campo de texto, vuelve al botón que lo abrió al
 * cerrar), bucle de tabulación propio, Escape cierra, región `aria-live` para los mensajes
 * nuevos. reservar_clase/cancelar_reserva del asistente solo proponen: aquí se ve como una
 * tarjeta con "Confirmar"/"Cancelar", y solo al confirmar se llama de verdad a la API.
 */
export function AssistantChatPanel({
  onClose,
  onActionConfirmed,
}: {
  onClose: () => void;
  onActionConfirmed: () => void;
}) {
  const { token } = useAuth();
  const [history, setHistory] = useState<AssistantChatMessage[]>([]);
  const [display, setDisplay] = useState<DisplayMessage[]>([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [planForbidden, setPlanForbidden] = useState(false);
  const [pendingAction, setPendingAction] = useState<AssistantPendingAction | null>(null);

  const dialogRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const logRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const previouslyFocused = document.activeElement as HTMLElement | null;
    inputRef.current?.focus();
    return () => previouslyFocused?.focus();
  }, []);

  useEffect(() => {
    logRef.current?.scrollTo({ top: logRef.current.scrollHeight });
  }, [display, pendingAction, sending]);

  function addMessage(role: "user" | "assistant", content: string) {
    setDisplay((prev) => [...prev, { id: crypto.randomUUID(), role, content }]);
  }

  function onDialogKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === "Escape") {
      event.preventDefault();
      onClose();
      return;
    }
    if (event.key !== "Tab" || !dialogRef.current) return;
    const focusable = dialogRef.current.querySelectorAll<HTMLElement>(
      'button:not([disabled]), [href], input:not([disabled]), [tabindex]:not([tabindex="-1"])',
    );
    if (focusable.length === 0) return;
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }

  async function handleSend(event: FormEvent) {
    event.preventDefault();
    if (!token || !input.trim() || sending) return;

    const userMessage: AssistantChatMessage = { role: "user", content: input.trim() };
    const nextHistory = trimHistory([...history, userMessage]);
    setHistory(nextHistory);
    addMessage("user", userMessage.content);
    setInput("");
    setSending(true);
    setError(null);
    try {
      const result = await api.assistantChat(token, { messages: nextHistory });
      setHistory((prev) => trimHistory([...prev, { role: "assistant", content: result.reply }]));
      addMessage("assistant", result.reply);
      setPendingAction(result.pendingAction);
    } catch (e) {
      if (e instanceof ApiError && e.status === 403) {
        setPlanForbidden(true);
      } else {
        setError(e instanceof ApiError ? e.message : "No se pudo hablar con el asistente. Inténtalo de nuevo.");
      }
    } finally {
      setSending(false);
    }
  }

  async function handleConfirm() {
    if (!token || !pendingAction || sending) return;
    setSending(true);
    setError(null);
    try {
      const result = await api.assistantChat(token, { messages: [], confirmAction: pendingAction });
      setHistory((prev) => trimHistory([...prev, { role: "assistant", content: result.reply }]));
      addMessage("assistant", result.reply);
      setPendingAction(null);
      onActionConfirmed();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "No se pudo confirmar. Inténtalo de nuevo.");
    } finally {
      setSending(false);
      // El botón "Confirmar" tenía el foco y va a desaparecer del DOM: lo devolvemos al
      // campo de texto para no dejar el foco perdido en <body> tras la actualización.
      inputRef.current?.focus();
    }
  }

  function handleDecline() {
    addMessage("assistant", "De acuerdo, no he hecho nada.");
    setPendingAction(null);
    inputRef.current?.focus();
  }

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/40" onClick={onClose}>
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-label="Asistente de reservas"
        onKeyDown={onDialogKeyDown}
        onClick={(event) => event.stopPropagation()}
        className="flex h-full w-full max-w-md flex-col border-l border-border bg-surface"
      >
        <header className="flex items-center justify-between border-b border-border px-4 py-3.5">
          <h2 className="font-display text-xl font-bold uppercase tracking-tight">Asistente de reservas</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar el asistente"
            className="rounded-control p-2 text-text-muted hover:text-text"
          >
            <X size={20} aria-hidden="true" />
          </button>
        </header>

        <div ref={logRef} aria-live="polite" role="log" className="flex flex-1 flex-col gap-3 overflow-y-auto px-4 py-4">
          {display.length === 0 && (
            <p className="text-sm text-text-muted">
              Pregúntame por el horario, tus reservas, o pídeme que reserves o canceles una clase.
            </p>
          )}

          {display.map((message) => (
            <div
              key={message.id}
              className={cn(
                "max-w-[85%] rounded-card px-3.5 py-2.5 text-sm",
                message.role === "user"
                  ? "self-end bg-accent text-on-accent"
                  : "self-start border border-border bg-surface-2 text-text",
              )}
            >
              {message.content}
            </div>
          ))}

          {pendingAction && (
            <div className="w-full max-w-[85%] self-start rounded-card border border-accent bg-surface-2 p-3.5">
              <p className="text-sm text-text">{pendingAction.summary}</p>
              <div className="mt-3 flex gap-2">
                <Button type="button" variant="primary" size="sm" disabled={sending} onClick={handleConfirm}>
                  Confirmar
                </Button>
                <Button type="button" variant="secondary" size="sm" disabled={sending} onClick={handleDecline}>
                  Cancelar
                </Button>
              </div>
            </div>
          )}

          {sending && (
            <div className="flex items-center gap-2 self-start text-xs text-text-muted">
              <span className="flex gap-1" aria-hidden="true">
                <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-text-dim [animation-delay:-0.3s]" />
                <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-text-dim [animation-delay:-0.15s]" />
                <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-text-dim" />
              </span>
              <span role="status">El asistente está escribiendo</span>
            </div>
          )}
        </div>

        {error && (
          <p
            role="alert"
            className="mx-4 mb-2 rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger"
          >
            {error}
          </p>
        )}

        {planForbidden ? (
          <div className="border-t border-border p-4">
            <p className="text-sm text-text-muted">Tu tarifa no incluye el asistente con IA.</p>
            <Link href="/cuenta/cuota/cambiar-tarifa" className="mt-2 inline-block text-sm text-accent-text hover:underline">
              Subir de tarifa
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSend} className="flex items-center gap-2 border-t border-border p-3">
            <label htmlFor="assistant-chat-input" className="sr-only">
              Escribe tu mensaje
            </label>
            <Input
              id="assistant-chat-input"
              ref={inputRef}
              type="text"
              value={input}
              maxLength={2000}
              disabled={sending}
              onChange={(event) => setInput(event.target.value)}
              placeholder="Escribe tu mensaje"
              className="h-11"
            />
            <Button type="submit" variant="primary" disabled={sending || !input.trim()}>
              <Send size={18} aria-hidden="true" />
              <span className="sr-only">Enviar</span>
            </Button>
          </form>
        )}
      </div>
    </div>
  );
}
