import { Dumbbell } from "lucide-react";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";

/**
 * El entrenador con IA (generar rutina, ajuste semanal) llega en el Trozo 5. La rutina de
 * hoy ya generada por el seed se ve en "Inicio"; aquí solo avisamos de que esto está en
 * camino, en vez de enseñar una página a medias.
 */
export default function MiEntrenamientoPage() {
  return (
    <main className="mx-auto max-w-2xl px-4 py-16 text-center sm:px-6">
      <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full border border-border bg-surface">
        <Dumbbell size={24} className="text-accent-text" aria-hidden="true" />
      </div>
      <Badge tone="neutral" className="mt-4">
        Próximamente
      </Badge>
      <h1 className="mt-3 font-display text-4xl font-extrabold uppercase leading-none tracking-tight">
        Tu entrenador con IA llega pronto
      </h1>
      <Card className="mt-6 text-left">
        <p className="text-sm text-text-muted">
          Aquí podrás configurar tu objetivo y tu nivel, generar una rutina semanal nueva y
          ajustarla según cómo te hayan sentado las últimas sesiones. Mientras tanto, tu
          rutina de esta semana ya está lista: puedes verla y marcar las series hechas desde{" "}
          <strong className="text-text">Inicio</strong>.
        </p>
      </Card>
    </main>
  );
}
