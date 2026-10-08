"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { CalendarCheck, CreditCard, Dumbbell, Home, User } from "lucide-react";
import { cn } from "@/lib/cn";

const ITEMS = [
  { href: "/cuenta", label: "Inicio", icon: Home },
  { href: "/horario", label: "Reservar", icon: CalendarCheck },
  { href: "/cuenta/entrenamiento", label: "Entrenador", icon: Dumbbell },
  { href: "/cuenta/cuota", label: "Mi cuota", icon: CreditCard },
  { href: "/cuenta/perfil", label: "Perfil", icon: User },
];

/** Barra inferior de 5 pestañas en móvil (DESIGN.md §8: la referencia "le gusta como está"). */
export function MobileAccountNav() {
  const pathname = usePathname();

  return (
    <nav
      className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-surface md:hidden"
      aria-label="Navegación de tu cuenta"
    >
      <div className="flex">
        {ITEMS.map((item) => {
          const active = pathname === item.href;
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex flex-1 flex-col items-center gap-1 py-2.5 text-xs font-medium",
                active ? "text-accent-text" : "text-text-muted",
              )}
            >
              <Icon size={20} aria-hidden="true" />
              {item.label}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
