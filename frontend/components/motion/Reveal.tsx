"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";
import { cn } from "@/lib/cn";

/**
 * Aparición suave al hacer scroll (Trozo 7b, DESIGN.md §6). Con `prefers-reduced-motion`
 * activo, `motion-safe:` hace que ni el estado "oculto" ni la transición lleguen a aplicarse:
 * el contenido sale visible desde el primer render, sin depender de si el observador ya
 * disparó o no. Usa opacidad y `translate` (nunca `height`/`display`), así que no hay salto de
 * diseño: el hueco en el documento ya existe desde el principio, solo cambia su apariencia.
 */
export function Reveal({ children, className }: { children: ReactNode; className?: string }) {
  const ref = useRef<HTMLDivElement>(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true);
          observer.disconnect();
        }
      },
      { threshold: 0.15, rootMargin: "0px 0px -10% 0px" },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  return (
    <div
      ref={ref}
      className={cn(
        "motion-safe:transition-all motion-safe:duration-700 motion-safe:ease-out",
        visible ? "motion-safe:opacity-100 motion-safe:translate-y-0" : "motion-safe:opacity-0 motion-safe:translate-y-4",
        className,
      )}
    >
      {children}
    </div>
  );
}
