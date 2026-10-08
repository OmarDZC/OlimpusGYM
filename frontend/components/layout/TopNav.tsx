"use client";

import Link from "next/link";
import { Menu, X } from "lucide-react";
import { useState } from "react";
import { Logo } from "@/components/Logo";
import { ThemeToggle } from "@/components/ThemeToggle";
import { Button } from "@/components/ui/Button";
import { Dropdown } from "@/components/ui/Dropdown";

const navLinks = [
  { label: "Horario", href: "/horario" },
  { label: "Entrenador IA", href: "/entrenador-ia" },
  { label: "Tarifas", href: "/tarifas" },
  { label: "Contacto", href: "/contacto" },
];

const classDisciplines = [
  { label: "CrossFit", href: "/horario?tipo=crossfit" },
  { label: "Fuerza", href: "/horario?tipo=fuerza" },
  { label: "Yoga", href: "/horario?tipo=yoga" },
  { label: "Boxeo", href: "/horario?tipo=boxeo" },
  { label: "Spinning", href: "/horario?tipo=spinning" },
  { label: "Halterofilia", href: "/horario?tipo=halterofilia" },
];

/** Navegación superior sin barra lateral (DESIGN.md §4 y §8). Hamburguesa en móvil. */
export function TopNav() {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <header className="sticky top-0 z-40 border-b border-border bg-bg/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
        <Link href="/" aria-label="Ir a inicio" className="shrink-0">
          <Logo />
        </Link>

        <nav className="hidden items-center gap-1 md:flex" aria-label="Navegación principal">
          <Dropdown label="Clases" items={classDisciplines} />
          {navLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="whitespace-nowrap rounded-control px-3 py-2 text-sm font-medium text-text-muted transition-colors hover:text-text"
            >
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="hidden items-center gap-3 md:flex">
          <ThemeToggle />
          <Button href="/acceso" variant="secondary" size="sm">
            Acceder
          </Button>
          <Button href="/acceso?modo=registro" variant="primary" size="sm">
            Apúntate
          </Button>
        </div>

        <button
          type="button"
          onClick={() => setMobileOpen((v) => !v)}
          aria-expanded={mobileOpen}
          aria-label={mobileOpen ? "Cerrar menú" : "Abrir menú"}
          className="inline-flex items-center justify-center rounded-control p-2 text-text md:hidden"
        >
          {mobileOpen ? <X size={22} /> : <Menu size={22} />}
        </button>
      </div>

      {mobileOpen && (
        <nav className="border-t border-border px-4 py-4 md:hidden" aria-label="Navegación móvil">
          <ul className="flex flex-col gap-1">
            {[{ label: "Clases", href: "/horario" }, ...navLinks].map((link) => (
              <li key={link.href}>
                <Link
                  href={link.href}
                  onClick={() => setMobileOpen(false)}
                  className="block rounded-control px-3 py-2 text-sm font-medium text-text-muted hover:bg-surface hover:text-text"
                >
                  {link.label}
                </Link>
              </li>
            ))}
          </ul>
          <div className="mt-4 flex items-center gap-3">
            <ThemeToggle />
            <Button href="/acceso" variant="secondary" size="sm" className="flex-1">
              Acceder
            </Button>
            <Button href="/acceso?modo=registro" variant="primary" size="sm" className="flex-1">
              Apúntate
            </Button>
          </div>
        </nav>
      )}
    </header>
  );
}
