"use client";

import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { roleLabels } from "@/lib/roles";

export default function PerfilPage() {
  const { user, logout } = useAuth();

  if (!user) return null;

  return (
    <main className="mx-auto max-w-xl px-4 py-10 sm:px-6">
      <h1 className="font-display text-4xl font-extrabold uppercase leading-none tracking-tight">Perfil</h1>

      <Card className="mt-6">
        <p className="text-xs font-semibold uppercase tracking-wide text-text-muted">Nombre</p>
        <p className="mt-1 text-lg font-medium text-text">{user.fullName}</p>

        <p className="mt-4 text-xs font-semibold uppercase tracking-wide text-text-muted">Correo electrónico</p>
        <p className="mt-1 text-text">{user.email}</p>

        <p className="mt-4 text-xs font-semibold uppercase tracking-wide text-text-muted">Rol</p>
        <Badge tone="accent" className="mt-1">
          {roleLabels[user.role]}
        </Badge>
      </Card>

      <Button variant="secondary" onClick={logout} className="mt-6 w-full">
        Cerrar sesión
      </Button>
    </main>
  );
}
