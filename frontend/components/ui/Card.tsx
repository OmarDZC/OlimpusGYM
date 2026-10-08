import type { HTMLAttributes } from "react";
import { cn } from "@/lib/cn";

/** Tarjeta base: superficie + borde de 1 px, sin sombras difusas (DESIGN.md §4). */
export function Card({ className, children, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn("rounded-card border border-border bg-surface p-6", className)} {...props}>
      {children}
    </div>
  );
}
