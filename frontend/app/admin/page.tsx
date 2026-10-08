"use client";

import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { roleLabels } from "@/lib/roles";

export default function AdminPage() {
  const { user, logout } = useAuth();

  if (!user) return null;

  return (
    <main className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
      <Badge tone="neutral">Datos de ejemplo · demo</Badge>
      <h1 className="mt-4 font-display text-5xl font-extrabold uppercase leading-none tracking-tight">
        Panel de gestión
      </h1>
      <p className="mt-2 text-text-muted">
        El resumen, las clases, los socios y las reservas se construyen en el Trozo 4.
      </p>

      <Card className="mt-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-sm text-text-muted">Sesión iniciada como</p>
          <p className="font-medium text-text">
            {user.fullName} <Badge tone="accent">{roleLabels[user.role]}</Badge>
          </p>
        </div>
        <Button variant="secondary" onClick={logout}>
          Cerrar sesión
        </Button>
      </Card>
    </main>
  );
}
