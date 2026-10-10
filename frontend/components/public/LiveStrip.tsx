"use client";

import { useEffect, useRef, useState } from "react";
import * as api from "@/lib/api";
import type { PublicLiveStatus } from "@/lib/api";

const MADRID_TZ = "Europe/Madrid";
// Dentro de los 30-60 s que pide LOGIC.md §9.
const REFRESH_MS = 45_000;

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit", timeZone: MADRID_TZ });
}

/**
 * Cuenta hasta el valor real (Trozo 7b, DESIGN.md §6: "contadores que suben"), nunca un número
 * inventado: solo anima la llegada de una cifra que ya viene de `GET /public/live`. Con
 * `prefers-reduced-motion` activo, salta directa al valor final.
 */
function useCountUp(target: number): number {
  const [value, setValue] = useState(target);

  useEffect(() => {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      setValue(target);
      return;
    }
    const start = performance.now();
    const duration = 600;
    let raf: number;
    function tick(now: number) {
      const progress = Math.min(1, (now - start) / duration);
      setValue(Math.round(progress * target));
      if (progress < 1) raf = requestAnimationFrame(tick);
    }
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [target]);

  return value;
}

/**
 * Franja "En directo" de la portada (Trozo 7b, LOGIC.md §9): solo datos reales de
 * `GET /public/live`. Se actualiza sola cada 45 s y deja de pedir datos mientras la pestaña no
 * está visible (`document.visibilitychange`), para no gastar peticiones de fondo sin motivo. Un
 * fallo de red solo cambia el texto por un aviso discreto, nunca rompe el resto de la página.
 */
export function LiveStrip() {
  const [status, setStatus] = useState<PublicLiveStatus | null>(null);
  const [error, setError] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const classesRemainingToday = useCountUp(status?.classesRemainingToday ?? 0);
  const classesWithFreeSlotsNow = useCountUp(status?.classesWithFreeSlotsNow ?? 0);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const data = await api.fetchPublicLive();
        if (!cancelled) {
          setStatus(data);
          setError(false);
        }
      } catch {
        if (!cancelled) setError(true);
      }
    }

    function clearTimer() {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
        timerRef.current = null;
      }
    }

    function scheduleNext() {
      clearTimer();
      if (document.hidden) return;
      timerRef.current = setTimeout(async () => {
        await load();
        scheduleNext();
      }, REFRESH_MS);
    }

    function handleVisibilityChange() {
      if (document.hidden) {
        clearTimer();
      } else {
        load().then(scheduleNext);
      }
    }

    load().then(scheduleNext);
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      cancelled = true;
      clearTimer();
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, []);

  return (
    <div className="flex flex-wrap items-center gap-3 rounded-control border border-border bg-surface-2 px-4 py-3 text-sm">
      <span className="relative flex h-2.5 w-2.5 shrink-0" aria-hidden="true">
        <span className="motion-safe:absolute motion-safe:inline-flex motion-safe:h-full motion-safe:w-full motion-safe:animate-ping motion-safe:rounded-full motion-safe:bg-accent/60" />
        <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-accent" />
      </span>

      {status ? (
        <p className="text-text-muted">
          <span className="font-semibold text-text">En directo</span>
          {" · "}
          {status.classesRemainingToday === 0 ? (
            "No quedan clases por empezar hoy."
          ) : (
            <>
              {classesRemainingToday} clase{status.classesRemainingToday === 1 ? "" : "s"} por empezar hoy
              {status.nextClass && (
                <>
                  {" · Próxima: "}
                  {status.nextClass.name} a las {formatTime(status.nextClass.startsAt)} (
                  {status.nextClass.freeSlots} plazas libres)
                </>
              )}
              {" · "}
              {classesWithFreeSlotsNow} con plazas libres ahora
            </>
          )}
        </p>
      ) : error ? (
        <p className="text-text-dim">No se pudo cargar el estado en directo ahora mismo.</p>
      ) : (
        <p className="text-text-dim">Cargando el estado en directo...</p>
      )}
    </div>
  );
}
