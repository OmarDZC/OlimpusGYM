import { expect, test } from "@playwright/test";
import { DEMO_PASSWORD, registerViaUi, uniqueEmail } from "../support/helpers";

/**
 * Cambio de tarifa (Trozo 9b, LOGIC.md §12), de punta a punta: un socio con la tarifa Básica
 * (sin asistente con IA) lo intenta, ve el aviso con el enlace "Subir de tarifa", sube a
 * Completa al instante desde /cuenta/cuota/cambiar-tarifa, y el asistente queda desbloqueado.
 * Necesita el LLM simulado (`e2e/fake-llm`, ver `docker-compose.e2e.yml`).
 */
test("un socio con tarifa Básica sube a Completa y desbloquea el asistente con IA", async ({ page }) => {
  await registerViaUi(page, {
    email: uniqueEmail("e2e.sube.tarifa"),
    password: DEMO_PASSWORD,
    fullName: "Socia Sube Tarifa E2E",
    planCode: "BASICA",
  });
  await expect(page).toHaveURL(/\/cuenta$/);

  await page.goto("/horario");
  await page.getByRole("button", { name: "Reservar con IA" }).click();
  const dialog = page.getByRole("dialog", { name: "Asistente de reservas" });
  await expect(dialog).toBeVisible();

  await dialog.getByLabel("Escribe tu mensaje").fill("¿Qué clases hay esta semana?");
  await dialog.getByRole("button", { name: "Enviar" }).click();

  await expect(dialog.getByText("Tu tarifa no incluye el asistente con IA.")).toBeVisible();
  await dialog.getByRole("link", { name: "Subir de tarifa" }).click();

  await expect(page).toHaveURL(/\/cuenta\/cuota\/cambiar-tarifa$/);
  await page.getByTestId("plan-option-COMPLETA").getByRole("button", { name: "Subir ahora" }).click();
  await page.getByRole("dialog", { name: "Confirmar cambio de tarifa" }).getByRole("button", { name: "Confirmar" }).click();
  await expect(page).toHaveURL(/\/cuenta\/cuota$/);
  await expect(page.getByText("Tarifa Completa")).toBeVisible();

  await page.goto("/horario");
  await page.getByRole("button", { name: "Reservar con IA" }).click();
  const dialogAfterUpgrade = page.getByRole("dialog", { name: "Asistente de reservas" });
  await expect(dialogAfterUpgrade).toBeVisible();

  await dialogAfterUpgrade.getByLabel("Escribe tu mensaje").fill("¿Qué clases hay esta semana?");
  await dialogAfterUpgrade.getByRole("button", { name: "Enviar" }).click();

  // El asistente de IA simulado siempre acaba proponiendo reservar una clase (ver
  // e2e/fake-llm/server.js): que llegue hasta ahí, en vez del aviso de tarifa, confirma que
  // ya tiene acceso. No se confirma la reserva: no es el objetivo de este test.
  await expect(dialogAfterUpgrade.getByRole("button", { name: "Confirmar" })).toBeVisible({ timeout: 15_000 });
  await expect(dialogAfterUpgrade.getByText("Tu tarifa no incluye el asistente con IA.")).not.toBeVisible();
  await dialogAfterUpgrade.getByRole("button", { name: "Cancelar" }).click();
});
