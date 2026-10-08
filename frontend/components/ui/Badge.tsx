import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

type Tone = "neutral" | "accent" | "danger" | "warning";

const tones: Record<Tone, string> = {
  neutral: "border-border bg-surface-2 text-text-muted",
  accent: "border-accent/40 bg-accent/10 text-accent-text",
  danger: "border-danger/40 bg-danger/10 text-danger",
  warning: "border-warning/40 bg-warning/10 text-warning",
};

/** Etiqueta pequeña en mayúsculas, para usar con moderación (DESIGN.md §3). */
export function Badge({
  tone = "neutral",
  className,
  children,
}: {
  tone?: Tone;
  className?: string;
  children: ReactNode;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-semibold uppercase tracking-wide",
        tones[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}
