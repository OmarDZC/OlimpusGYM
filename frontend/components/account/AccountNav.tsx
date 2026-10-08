"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/cn";

const ITEMS = [
  { href: "/cuenta", label: "Inicio" },
  { href: "/cuenta/reservas", label: "Mis reservas" },
  { href: "/cuenta/entrenamiento", label: "Mi entrenamiento" },
  { href: "/cuenta/cuota", label: "Mi cuota" },
];

/** Submenú de la zona de socio (DESIGN.md §8). Solo en escritorio: en móvil manda la barra inferior. */
export function AccountNav() {
  const pathname = usePathname();

  return (
    <nav className="hidden border-b border-border bg-surface md:block" aria-label="Navegación de tu cuenta">
      <div className="mx-auto flex max-w-5xl gap-1 px-4 sm:px-6">
        {ITEMS.map((item) => {
          const active = pathname === item.href;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "border-b-2 px-3 py-3 text-sm font-medium transition-colors",
                active ? "border-accent text-text" : "border-transparent text-text-muted hover:text-text",
              )}
            >
              {item.label}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
