import type { PlanCode, ScheduleSession } from "./api";

/**
 * Solo para Server Components (Trozo 7a): dentro de Docker, el frontend no puede llegar al
 * backend por `localhost` (es otro contenedor), así que aquí se usa el nombre de servicio de la
 * red de Docker en vez de la URL pública que ya usa `lib/api.ts` para las llamadas del navegador.
 */
const API_URL = process.env.API_INTERNAL_URL ?? process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export type PublicPlan = {
  planCode: PlanCode;
  planName: string;
  priceCents: number;
  classesPerMonth: number | null;
  aiTrainer: boolean;
  aiAssistant: boolean;
};

export async function fetchPublicConfigServer(): Promise<{ demoMode: boolean }> {
  try {
    const response = await fetch(`${API_URL}/api/v1/public/config`, { cache: "no-store" });
    if (!response.ok) return { demoMode: false };
    return response.json();
  } catch {
    return { demoMode: false };
  }
}

export async function fetchPublicPlans(): Promise<PublicPlan[]> {
  // Sin caché: en la imagen de Docker, "next build" no tiene red hacia el backend (son
  // contenedores aparte), así que cachear la respuesta del build dejaría esta página servida
  // con una lista de tarifas vacía hasta la siguiente revalidación. Más simple y seguro pedirla
  // siempre en el momento, igual que ya hace fetchTodaySchedule.
  const response = await fetch(`${API_URL}/api/v1/public/plans`, { cache: "no-store" });
  if (!response.ok) {
    throw new Error("No se pudieron cargar las tarifas.");
  }
  return response.json();
}

function startOfDayUtc(date: Date): Date {
  return new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
}

/** Sesiones de hoy que no han empezado (el backend nunca devuelve las ya empezadas). */
export async function fetchTodaySchedule(): Promise<ScheduleSession[]> {
  const endOfToday = new Date(startOfDayUtc(new Date()).getTime() + 86_400_000);
  const response = await fetch(`${API_URL}/api/v1/public/schedule?to=${encodeURIComponent(endOfToday.toISOString())}`, {
    cache: "no-store",
  });
  if (!response.ok) {
    throw new Error("No se pudo cargar el horario de hoy.");
  }
  return response.json();
}
