import Link from "next/link";

const footerLinks = [
  { label: "Horario", href: "/horario" },
  { label: "Tarifas", href: "/tarifas" },
  { label: "Contacto", href: "/contacto" },
];

/**
 * Pie de página de la web pública (Trozo 7a, DESIGN.md §8). No repite el logo (ya sale una vez
 * en `TopNav`, DESIGN.md §1: "muéstralo una sola vez por pantalla").
 */
export function Footer() {
  return (
    <footer className="border-t border-border bg-surface">
      <div className="mx-auto flex max-w-6xl flex-col gap-8 px-4 py-10 sm:flex-row sm:items-start sm:justify-between sm:px-6">
        <div className="max-w-xs">
          <p className="font-display text-lg font-bold uppercase tracking-tight text-text">OlympusGYM</p>
          <p className="mt-2 text-sm text-text-muted">
            Gimnasio de demostración: nombres, cifras y testimonios de esta web son datos de ejemplo.
          </p>
        </div>

        <nav aria-label="Enlaces del pie de página" className="flex flex-col gap-2 text-sm">
          {footerLinks.map((link) => (
            <Link key={link.href} href={link.href} className="text-text-muted hover:text-text">
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="text-sm text-text-muted">
          <p>Calle del Hierro 12, 28045 Madrid (dirección de ejemplo)</p>
          <p className="mt-1">Sala de pesas: 06:00 a 23:00, todos los días</p>
        </div>
      </div>
      <div className="border-t border-border px-4 py-4 text-center text-xs text-text-dim sm:px-6">
        OlympusGYM — proyecto de portfolio con datos de ejemplo.
      </div>
    </footer>
  );
}
