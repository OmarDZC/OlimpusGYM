"use client";

import { LogOut } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef, useState, type KeyboardEvent } from "react";
import { useAuth } from "@/components/auth/AuthProvider";
import { Badge } from "@/components/ui/Badge";
import { cn } from "@/lib/cn";
import { roleLabels } from "@/lib/roles";

const ITEMS = [
  { href: "/admin", label: "Resumen" },
  { href: "/admin/clases", label: "Clases" },
  { href: "/admin/socios", label: "Socios" },
  { href: "/admin/reservas", label: "Reservas" },
  { href: "/admin/mensajes", label: "Mensajes" },
];

function initials(fullName: string): string {
  // "Javier M. (dueño)" -> "JM": lo de entre paréntesis no son iniciales de nadie.
  return fullName
    .replace(/\([^)]*\)/g, " ")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

function TabLink({ href, label, active, scrollable }: { href: string; label: string; active: boolean; scrollable?: boolean }) {
  const ref = useRef<HTMLAnchorElement>(null);

  useEffect(() => {
    if (active && scrollable) {
      ref.current?.scrollIntoView({ inline: "center", block: "nearest" });
    }
    // Solo al montar: centra la pestaña activa una vez, sin pelearse con el scroll del socio.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <Link
      ref={ref}
      href={href}
      aria-current={active ? "page" : undefined}
      className={cn(
        "shrink-0 whitespace-nowrap rounded-control px-3 py-2 text-sm font-medium transition-colors",
        active ? "bg-accent text-on-accent" : "text-text-muted hover:text-text",
      )}
    >
      {label}
    </Link>
  );
}

/** Menú con el nombre, el rol y "Salir": en el avatar, para no ocupar una fila propia
 * (sobre todo en móvil, donde la cabecera no puede permitirse una fila solo para esto). */
function AccountMenu({ fullName, role }: { fullName: string; role: string }) {
  const { logout } = useAuth();
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    function onClickOutside(event: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", onClickOutside);
    return () => document.removeEventListener("mousedown", onClickOutside);
  }, []);

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === "Escape") {
      event.preventDefault();
      setOpen(false);
      buttonRef.current?.focus();
    }
  }

  return (
    <div className="relative" ref={rootRef} onKeyDown={onKeyDown}>
      <button
        ref={buttonRef}
        type="button"
        aria-haspopup="true"
        aria-expanded={open}
        aria-label={`Cuenta de ${fullName}, ${role}`}
        onClick={() => setOpen((v) => !v)}
        className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-border bg-surface-2 text-sm font-semibold text-text"
      >
        {initials(fullName)}
      </button>

      {open && (
        <div
          role="menu"
          className="absolute right-0 top-full z-20 mt-2 w-52 rounded-card border border-border bg-surface p-3 shadow-lg"
        >
          <p className="truncate text-sm font-medium text-text">{fullName}</p>
          <Badge tone="accent" className="mt-1">
            {role}
          </Badge>
          <button
            type="button"
            role="menuitem"
            onClick={logout}
            className="mt-3 flex w-full items-center gap-2 rounded-control px-2 py-2 text-sm text-text-muted hover:bg-surface-2 hover:text-text"
          >
            <LogOut size={16} aria-hidden="true" />
            Salir
          </button>
        </div>
      )}
    </div>
  );
}

/**
 * Navegación superior del panel de gestión (DESIGN.md §8: sin barra lateral, rol visible
 * bajo el avatar). Dueño y staff ven los mismos apartados (LOGIC.md §2 les da acceso a
 * todos); lo que cambia por rol es el contenido dentro de cada uno (ingresos y cambio
 * de tarifa son solo para el dueño), no la lista de pestañas.
 *
 * En móvil la cabecera se reparte en dos filas compactas (avatar arriba, pestañas con
 * desplazamiento horizontal debajo) en vez de la única fila de escritorio, que no cabía.
 */
export function AdminNav() {
  const pathname = usePathname();
  const { user } = useAuth();

  return (
    <header className="border-b border-border bg-surface">
      <div className="mx-auto max-w-6xl px-4 py-3 sm:px-6">
        <div className="hidden items-center justify-between gap-4 md:flex">
          <nav className="flex flex-wrap gap-1" aria-label="Navegación del panel de gestión">
            {ITEMS.map((item) => (
              <TabLink key={item.href} href={item.href} label={item.label} active={pathname === item.href} />
            ))}
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

        <div className="flex items-center justify-between gap-3 md:hidden">
          <div className="relative min-w-0 flex-1">
            <nav
              className="flex gap-1 overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
              aria-label="Navegación del panel de gestión"
            >
              {ITEMS.map((item) => (
                <TabLink key={item.href} href={item.href} label={item.label} active={pathname === item.href} scrollable />
              ))}
            </nav>
            <div className="pointer-events-none absolute inset-y-0 left-0 w-6 bg-gradient-to-r from-surface to-transparent" />
            <div className="pointer-events-none absolute inset-y-0 right-0 w-6 bg-gradient-to-l from-surface to-transparent" />
          </div>

          {user && <AccountMenu fullName={user.fullName} role={roleLabels[user.role]} />}
        </div>
      </div>
    </header>
  );
}
