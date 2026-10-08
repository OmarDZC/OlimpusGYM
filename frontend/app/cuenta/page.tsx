"use client";

import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";

export default function CuentaPage() {
  const { user, logout } = useAuth();

  if (!user) return null;

  const firstName = user.fullName.split(" ")[0];

  return (
    <main className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
      <Badge tone="neutral">Datos de ejemplo · demo</Badge>
      <h1 className="mt-4 font-display text-5xl font-extrabold uppercase leading-none tracking-tight">
        Hola, {firstName}
      </h1>
      <p className="mt-2 text-text-muted">
        Socio de OlympusGYM. Tus reservas, tu rutina y tu cuota llegan en los próximos trozos del roadmap.
      </p>

      <Card className="mt-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-sm text-text-muted">Sesión iniciada como</p>
          <p className="font-medium text-text">{user.email}</p>
        </div>
        <Button variant="secondary" onClick={logout}>
          Cerrar sesión
        </Button>
      </Card>
    </main>
  );
}
