import type { APIRequestContext, Page } from "@playwright/test";
import { expect } from "@playwright/test";

/** Mismas credenciales de demo que `docs/PROGRESS.md` y el seed de `dev`/`demo`. */
export const DEMO_PASSWORD = "Demo1234!";
export const OWNER_EMAIL = "javier@olympusgym.test";
export const STAFF_EMAIL = "marta@olympusgym.test";
export const MEMBER_EMAIL = "carlos@olympusgym.test";

export const API_URL = process.env.E2E_API_URL ?? "http://localhost:8080";

/** Correo único por ejecución: evita colisiones con "correo ya registrado" entre tests o reintentos. */
export function uniqueEmail(prefix: string): string {
  return `${prefix}.${Date.now()}.${Math.floor(Math.random() * 100000)}@olympusgym.test`;
}

export function uniqueName(prefix: string): string {
  return `${prefix} ${Date.now()}-${Math.floor(Math.random() * 10000)}`;
}

/** Login real a través del formulario de /acceso: es el camino que de verdad usan los socios. */
export async function loginViaUi(page: Page, email: string, password: string): Promise<void> {
  await page.goto("/acceso");
  await page.getByLabel("Correo electrónico").fill(email);
  await page.getByLabel("Contraseña").fill(password);
  await page.getByRole("button", { name: "Entrar a mi cuenta" }).click();
}

export async function logout(page: Page): Promise<void> {
  await page.getByRole("button", { name: "Salir" }).click();
  await expect(page.getByRole("link", { name: "Acceder" })).toBeVisible();
}

/**
 * Registro real a través del formulario de /acceso. Se usa para los tests que necesitan
 * un socio nuevo y limpio (sin reservas ni rutina previas del seed).
 */
export async function registerViaUi(
  page: Page,
  { email, password, fullName, planCode }: { email: string; password: string; fullName: string; planCode?: "BASICA" | "COMPLETA" | "ATLETA_PRO" },
): Promise<void> {
  await page.goto("/acceso?modo=registro");
  await page.getByRole("tab", { name: "Crear cuenta" }).click();
  await page.getByLabel("Nombre completo").fill(fullName);
  await page.getByLabel("Correo electrónico").fill(email);
  await page.getByLabel("Contraseña").fill(password);
  // La tarifa por defecto del formulario es Completa (incluye entrenador IA y asistente);
  // solo hace falta tocar el selector cuando el test necesita otra tarifa de partida.
  if (planCode) {
    await page.getByLabel("Tarifa").selectOption(planCode);
  }
  await page.getByRole("button", { name: "Crear mi cuenta" }).click();
}

/**
 * Login por API (sin pasar por el navegador): solo para preparar datos de prueba
 * (crear una clase como OWNER), nunca para el flujo que el test está verificando.
 */
export async function apiLogin(
  request: APIRequestContext,
  email: string,
  password: string,
): Promise<string> {
  const response = await request.post(`${API_URL}/api/v1/auth/login`, {
    data: { email, password },
  });
  if (!response.ok()) {
    throw new Error(`Login de API falló para ${email}: ${response.status()} ${await response.text()}`);
  }
  const body = await response.json();
  return body.token as string;
}

/**
 * Crea una clase de prueba vía API (como OWNER), varios días en el futuro para que
 * quepa en la ventana de reserva de cualquier tarifa y en los 14 días que muestra /horario.
 * Devuelve su id (para los `data-testid` de la interfaz), nombre único y fecha de inicio.
 */
export async function createTestSession(
  request: APIRequestContext,
  ownerToken: string,
  { daysAhead = 3, capacity = 5 }: { daysAhead?: number; capacity?: number } = {},
): Promise<{ id: string; name: string; startsAt: Date }> {
  const name = uniqueName("E2E Clase");
  const startsAt = new Date(Date.now() + daysAhead * 86_400_000);
  startsAt.setUTCHours(10, 0, 0, 0);
  const endsAt = new Date(startsAt.getTime() + 45 * 60_000);

  const response = await request.post(`${API_URL}/api/v1/admin/sessions`, {
    headers: { Authorization: `Bearer ${ownerToken}` },
    data: {
      name,
      discipline: "CrossFit",
      room: "Sala E2E",
      startsAt: startsAt.toISOString(),
      endsAt: endsAt.toISOString(),
      capacity,
      repeatWeeks: 1,
    },
  });
  if (!response.ok()) {
    throw new Error(`No se pudo crear la clase de prueba: ${response.status()} ${await response.text()}`);
  }
  const [created] = await response.json();
  return { id: created.id as string, name, startsAt };
}

/**
 * Selecciona en /horario la pestaña del día que corresponde a la fecha dada. El selector de
 * días de la interfaz construye sus `data-testid` a partir de la fecha UTC (igual que aquí);
 * como `createTestSession` también fija la hora en UTC, ambos coinciden sin pasar por zona horaria.
 */
export async function goToScheduleDay(page: Page, date: Date): Promise<void> {
  await page.goto("/horario");
  await page.getByTestId(`schedule-day-${date.toISOString().slice(0, 10)}`).click();
}
