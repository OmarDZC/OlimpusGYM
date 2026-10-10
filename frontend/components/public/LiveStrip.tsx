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
 * Franja "En directo" de la portada (Trozo 7b, LOGIC.md §9): solo datos reales de
 * `GET /public/live`. Se actualiza sola cada 45 s y deja de pedir datos mientras la pestaña no
 * está visible (`document.visibilitychange`), para no gastar peticiones de fondo sin motivo. Un
 * fallo de red solo cambia el texto por un aviso discreto, nunca rompe el resto de la página.
 */
export function LiveStrip() {
  const [status, setStatus] = useState<PublicLiveStatus | null>(null);
  const [error, setError] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

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
              {status.classesRemainingToday} clase{status.classesRemainingToday === 1 ? "" : "s"} por empezar hoy
              {status.nextClass && (
                <>
                  {" · Próxima: "}
                  {status.nextClass.name} a las {formatTime(status.nextClass.startsAt)} (
                  {status.nextClass.freeSlots} plazas libres)
                </>
              )}
              {" · "}
              {status.classesWithFreeSlotsNow} con plazas libres ahora
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
