"use client";

import { useId, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Textarea } from "@/components/ui/Textarea";
import * as api from "@/lib/api";
import { ApiError } from "@/lib/api";

/**
 * Formulario de contacto (Trozo 7a). El campo `website` es la trampa contra bots: se mantiene
 * fuera de la vista con CSS (nunca `display:none`, que algunos rastreadores simples ya ignoran)
 * y fuera del tabulador y de los lectores de pantalla, para que no lo note nadie real.
 */
export function ContactForm() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [website, setWebsite] = useState("");

  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [sent, setSent] = useState(false);

  const nameId = useId();
  const emailId = useId();
  const messageId = useId();
  const websiteId = useId();

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);
    setFieldErrors({});
    setSubmitting(true);
    try {
      await api.submitContact({ name, email, message, website });
      setSent(true);
      setName("");
      setEmail("");
      setMessage("");
    } catch (error) {
      if (error instanceof ApiError) {
        setFormError(error.message);
        setFieldErrors(error.fieldErrors ?? {});
      } else {
        setFormError("No se pudo conectar con el servidor. Inténtalo de nuevo.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (sent) {
    return (
      <p role="status" className="rounded-control border border-accent/40 bg-accent/10 px-3.5 py-2.5 text-sm text-text">
        Mensaje enviado. Te responderemos en cuanto podamos.
      </p>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      <Field label="Nombre" htmlFor={nameId} error={fieldErrors.name}>
        <Input
          id={nameId}
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          maxLength={120}
          aria-invalid={Boolean(fieldErrors.name)}
          aria-describedby={fieldErrors.name ? `${nameId}-error` : undefined}
        />
      </Field>

      <Field label="Correo electrónico" htmlFor={emailId} error={fieldErrors.email}>
        <Input
          id={emailId}
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          maxLength={180}
          aria-invalid={Boolean(fieldErrors.email)}
          aria-describedby={fieldErrors.email ? `${emailId}-error` : undefined}
        />
      </Field>

      <Field label="Mensaje" htmlFor={messageId} error={fieldErrors.message}>
        <Textarea
          id={messageId}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          required
          rows={5}
          maxLength={2000}
          aria-invalid={Boolean(fieldErrors.message)}
          aria-describedby={fieldErrors.message ? `${messageId}-error` : undefined}
        />
      </Field>

      {/* Campo trampa: invisible y fuera del tabulador para una persona, visible para un bot. */}
      <div aria-hidden="true" style={{ position: "absolute", left: "-9999px", top: "auto", width: 1, height: 1, overflow: "hidden" }}>
        <label htmlFor={websiteId}>No rellenar este campo</label>
        <input
          id={websiteId}
          type="text"
          tabIndex={-1}
          autoComplete="off"
          value={website}
          onChange={(e) => setWebsite(e.target.value)}
        />
      </div>

      {formError && (
        <p role="alert" className="rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
          {formError}
        </p>
      )}

      <Button type="submit" variant="primary" disabled={submitting} className="w-full sm:w-auto">
        {submitting ? "Enviando..." : "Enviar mensaje"}
      </Button>
    </form>
  );
}
