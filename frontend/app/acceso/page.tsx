"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useId, useState } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { ApiError, fetchPublicConfig, type PlanCode } from "@/lib/api";
import { cn } from "@/lib/cn";
import { homeForRole, planOptions } from "@/lib/roles";

type Mode = "login" | "registro";

export default function AccesoPage() {
  return (
    <Suspense fallback={null}>
      <AccesoContent />
    </Suspense>
  );
}

function AccesoContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { status, user, login, register, demoLogin } = useAuth();

  const [mode, setMode] = useState<Mode>(searchParams.get("modo") === "registro" ? "registro" : "login");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [demoMode, setDemoMode] = useState(false);
  const [demoSubmitting, setDemoSubmitting] = useState<"MEMBER" | "OWNER" | null>(null);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [planCode, setPlanCode] = useState<PlanCode>("COMPLETA");

  const emailId = useId();
  const passwordId = useId();
  const fullNameId = useId();
  const planId = useId();

  useEffect(() => {
    if (status === "signed-in" && user) {
      router.replace(homeForRole(user.role));
    }
  }, [status, user, router]);

  useEffect(() => {
    fetchPublicConfig()
      .then((config) => setDemoMode(config.demoMode))
      .catch(() => setDemoMode(false));
  }, []);

  async function handleDemoLogin(role: "MEMBER" | "OWNER") {
    setFormError(null);
    setDemoSubmitting(role);
    try {
      const signedInUser = await demoLogin(role);
      router.replace(homeForRole(signedInUser.role));
    } catch {
      setFormError("No se pudo iniciar la sesión de demostración. Inténtalo de nuevo.");
    } finally {
      setDemoSubmitting(null);
    }
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);
    setFieldErrors({});
    setSubmitting(true);
    try {
      const signedInUser =
        mode === "login" ? await login(email, password) : await register({ email, password, fullName, planCode });
      router.replace(homeForRole(signedInUser.role));
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

  if (status === "loading" || status === "signed-in") {
    return null;
  }

  return (
    <main className="mx-auto grid min-h-[calc(100vh-4rem)] max-w-6xl gap-10 px-4 py-12 sm:px-6 lg:grid-cols-2 lg:items-center lg:py-16">
      <section className="order-2 lg:order-1">
        <Badge tone="neutral">Datos de ejemplo · demo</Badge>
        <h1 className="mt-4 font-display text-5xl font-extrabold uppercase leading-none tracking-tight sm:text-6xl">
          Bienvenido a tu <span className="text-accent-text">Olimpo</span>
        </h1>
        <p className="mt-4 max-w-md text-text-muted">
          Entra a tu zona de entrenamiento, gestiona tus clases y mantén tu constancia cada día.
        </p>
        <Link href="/" className="mt-6 inline-block text-sm text-text-muted underline-offset-4 hover:text-text hover:underline">
          ← Volver a la página principal
        </Link>
      </section>

      <section className="order-1 lg:order-2">
        <Card className="mx-auto w-full max-w-md">
          {demoMode && (
            <p className="mb-6 rounded-control border border-accent/40 bg-accent/10 px-3.5 py-2.5 text-sm text-text">
              Demostración: los datos son de ejemplo y se reinician cada noche.
            </p>
          )}

          <div role="tablist" aria-label="Acceso a tu cuenta" className="mb-6 grid grid-cols-2 gap-1 rounded-control bg-surface-2 p-1">
            <button
              type="button"
              role="tab"
              aria-selected={mode === "login"}
              onClick={() => setMode("login")}
              className={cn(
                "rounded-control py-2 text-sm font-semibold transition-colors",
                mode === "login" ? "bg-accent text-on-accent" : "text-text-muted hover:text-text",
              )}
            >
              Acceder
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={mode === "registro"}
              onClick={() => setMode("registro")}
              className={cn(
                "rounded-control py-2 text-sm font-semibold transition-colors",
                mode === "registro" ? "bg-accent text-on-accent" : "text-text-muted hover:text-text",
              )}
            >
              Crear cuenta
            </button>
          </div>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
            {mode === "registro" && (
              <Field label="Nombre completo" htmlFor={fullNameId} error={fieldErrors.fullName}>
                <Input
                  id={fullNameId}
                  type="text"
                  autoComplete="name"
                  required
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  aria-invalid={Boolean(fieldErrors.fullName)}
                  aria-describedby={fieldErrors.fullName ? `${fullNameId}-error` : undefined}
                />
              </Field>
            )}

            <Field label="Correo electrónico" htmlFor={emailId} error={fieldErrors.email}>
              <Input
                id={emailId}
                type="email"
                autoComplete="email"
                placeholder="tu@email.com"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                aria-invalid={Boolean(fieldErrors.email)}
                aria-describedby={fieldErrors.email ? `${emailId}-error` : undefined}
              />
            </Field>

            <Field label="Contraseña" htmlFor={passwordId} error={fieldErrors.password}>
              <Input
                id={passwordId}
                type="password"
                autoComplete={mode === "login" ? "current-password" : "new-password"}
                required
                minLength={mode === "registro" ? 8 : undefined}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                aria-invalid={Boolean(fieldErrors.password)}
                aria-describedby={fieldErrors.password ? `${passwordId}-error` : undefined}
              />
            </Field>

            {mode === "registro" && (
              <Field label="Tarifa" htmlFor={planId} error={fieldErrors.planCode}>
                <Select id={planId} value={planCode} onChange={(e) => setPlanCode(e.target.value as PlanCode)}>
                  {planOptions.map((plan) => (
                    <option key={plan.code} value={plan.code}>
                      {plan.label}
                    </option>
                  ))}
                </Select>
              </Field>
            )}

            {formError && (
              <p role="alert" className="rounded-control border border-danger/40 bg-danger/10 px-3.5 py-2.5 text-sm text-danger">
                {formError}
              </p>
            )}

            <Button type="submit" variant="primary" disabled={submitting} className="mt-2 w-full">
              {submitting ? "Un momento..." : mode === "login" ? "Entrar a mi cuenta" : "Crear mi cuenta"}
            </Button>
          </form>

          {demoMode && (
            <div className="mt-6 flex flex-col gap-2 border-t border-border pt-6">
              <Button
                type="button"
                variant="secondary"
                disabled={demoSubmitting !== null}
                onClick={() => handleDemoLogin("MEMBER")}
                className="w-full"
              >
                {demoSubmitting === "MEMBER" ? "Un momento..." : "Entrar como socio de demostración"}
              </Button>
              <Button
                type="button"
                variant="secondary"
                disabled={demoSubmitting !== null}
                onClick={() => handleDemoLogin("OWNER")}
                className="w-full"
              >
                {demoSubmitting === "OWNER" ? "Un momento..." : "Entrar como dueño de demostración"}
              </Button>
            </div>
          )}
        </Card>
      </section>
    </main>
  );
}
