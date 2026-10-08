import { Dumbbell } from "lucide-react";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";

/**
 * Configurar y generar la rutina pasa en /entrenador-ia (configurador real, Trozo 5); aquí
 * solo se enlaza y se recuerda dónde ver y marcar series, para no duplicar esa pantalla.
 */
export default function MiEntrenamientoPage() {
  return (
    <main className="mx-auto max-w-2xl px-4 py-16 text-center sm:px-6">
      <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full border border-border bg-surface">
        <Dumbbell size={24} className="text-accent-text" aria-hidden="true" />
      </div>
      <Badge tone="neutral" className="mt-4">
        Entrenador con IA
      </Badge>
      <h1 className="mt-3 font-display text-4xl font-extrabold uppercase leading-none tracking-tight">
        Genera o cambia tu rutina semanal
      </h1>
      <Card className="mt-6 text-left">
        <p className="text-sm text-text-muted">
          Configura tu objetivo, tu nivel y los días que puedes entrenar en{" "}
          <strong className="text-text">Entrenador IA</strong> y genera tu rutina de esta semana. En cuanto esté
          lista, puedes verla y marcar las series hechas desde <strong className="text-text">Inicio</strong>.
        </p>
        <Button href="/entrenador-ia" variant="primary" className="mt-4">
          Ir a Entrenador IA
        </Button>
      </Card>
    </main>
  );
}
