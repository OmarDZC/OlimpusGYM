"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { cn } from "@/lib/cn";
import { roleLabels } from "@/lib/roles";

const ITEMS = [
  { href: "/admin", label: "Resumen" },
  { href: "/admin/clases", label: "Clases" },
  { href: "/admin/socios", label: "Socios" },
  { href: "/admin/reservas", label: "Reservas" },
];

function initials(fullName: string): string {
  return fullName
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

/**
 * Navegación superior del panel de gestión (DESIGN.md §8: sin barra lateral, rol visible
 * bajo el avatar). Dueño y staff ven los mismos 4 apartados (LOGIC.md §2 les da acceso a
 * los cuatro); lo que cambia por rol es el contenido dentro de cada uno (ingresos y cambio
 * de tarifa son solo para el dueño), no la lista de pestañas.
 */
export function AdminNav() {
  const pathname = usePathname();
  const { user } = useAuth();

  return (
    <header className="border-b border-border bg-surface">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-3 sm:px-6">
        <nav className="flex flex-wrap gap-1" aria-label="Navegación del panel de gestión">
          {ITEMS.map((item) => {
            const active = pathname === item.href;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "rounded-control px-3 py-2 text-sm font-medium transition-colors",
                  active ? "bg-accent text-on-accent" : "text-text-muted hover:text-text",
                )}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        {user && (
          <div className="flex items-center gap-3">
            <div className="text-right">
              <p className="text-sm font-medium leading-tight text-text">{user.fullName}</p>
              <Badge tone="accent" className="mt-0.5">
                {roleLabels[user.role]}
              </Badge>
            </div>
            <div
              aria-hidden="true"
              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-border bg-surface-2 text-sm font-semibold text-text"
            >
              {initials(user.fullName)}
            </div>
          </div>
        )}
      </div>
    </header>
  );
}
