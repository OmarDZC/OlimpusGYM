"use client";

import { useEffect, useState } from "react";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

type ApiState = "loading" | "ok" | "down";

const statusLabel: Record<ApiState, string> = {
  loading: "Comprobando...",
  ok: "Conectado",
  down: "Sin conexión",
};

const statusTone: Record<ApiState, "warning" | "accent" | "danger"> = {
  loading: "warning",
  ok: "accent",
  down: "danger",
};

export default function Home() {
  const [state, setState] = useState<ApiState>("loading");

  useEffect(() => {
    fetch(`${API_URL}/api/v1/public/ping`)
      .then((r) => setState(r.ok ? "ok" : "down"))
      .catch(() => setState("down"));
  }, []);

  return (
    <main className="mx-auto flex min-h-[calc(100vh-4rem)] max-w-3xl flex-col items-center justify-center gap-8 px-4 py-16 text-center sm:px-6">
      <Badge tone="neutral">Datos de ejemplo · demo</Badge>

      <h1 className="font-display text-5xl font-extrabold uppercase leading-none tracking-tight sm:text-6xl">
        Forja tu propio <span className="text-accent-text">Olimpo</span>
      </h1>

      <p className="max-w-xl text-base text-text-muted">
        Reservas de clases con aforo, entrenador con inteligencia artificial y gestión completa
        para un gimnasio real.
      </p>

      <div className="flex flex-wrap items-center justify-center gap-3">
        <Button href="/horario" variant="primary">
          Ver horario de clases
        </Button>
        <Button href="/entrenador-ia" variant="secondary">
          Entrenador con IA
        </Button>
      </div>

      <Card className="w-full max-w-sm">
        <div className="flex items-center justify-between gap-4">
          <span className="text-sm font-medium text-text-muted">Estado del backend</span>
          <Badge tone={statusTone[state]}>{statusLabel[state]}</Badge>
        </div>
      </Card>
    </main>
  );
}
