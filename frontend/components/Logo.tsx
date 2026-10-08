import { cn } from "@/lib/cn";

/**
 * Logo de marca: OLYMPUS + trazo de pulso (en forma de cima de montaña) + GYM.
 * Texto/SVG, nunca una imagen (DESIGN.md §1). Debe mostrarse una sola vez por pantalla.
 */
export function Logo({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-2 font-display text-xl font-extrabold uppercase tracking-tight text-text",
        className,
      )}
    >
      OLYMPUS
      <svg width="40" height="20" viewBox="0 0 40 20" fill="none" aria-hidden="true" className="shrink-0">
        <path
          d="M1 11H8L11 4L16 17L20 7L23 13L26 11H39"
          stroke="var(--color-accent)"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
      <span className="text-accent-text">GYM</span>
    </span>
  );
}
