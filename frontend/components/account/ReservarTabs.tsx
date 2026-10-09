"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/cn";

const TABS = [
  { href: "/horario", label: "Horario" },
  { href: "/cuenta/reservas", label: "Mis reservas" },
];

/**
 * Enlaces entre /horario y /cuenta/reservas, con pinta de pestañas. Son rutas reales (no un
 * panel que cambia con JavaScript), así que cada una se puede enlazar y usar el botón atrás del
 * navegador con normalidad: por eso son `<Link>` con `aria-current`, no un `role="tablist"`
 * (ese patrón ARIA es para paneles en la misma página, no para navegar). En móvil es la única
 * forma de llegar a "Mis reservas": no es una de las 5 pestañas de la barra inferior.
 */
export function ReservarTabs() {
  const pathname = usePathname();

  return (
    <nav aria-label="Reservar" className="mb-6 inline-flex gap-1 rounded-control bg-surface-2 p-1">
      {TABS.map((tab) => {
        const active = pathname === tab.href;
        return (
          <Link
            key={tab.href}
            href={tab.href}
            aria-current={active ? "page" : undefined}
            className={cn(
              "rounded-control px-4 py-2 text-sm font-semibold transition-colors",
              active ? "bg-accent text-on-accent" : "text-text-muted hover:text-text",
            )}
          >
            {tab.label}
          </Link>
        );
      })}
    </nav>
  );
}
