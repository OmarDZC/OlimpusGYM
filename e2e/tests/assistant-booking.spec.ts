import { expect, test } from "@playwright/test";
import {
  apiLogin,
  createTestSession,
  DEMO_PASSWORD,
  OWNER_EMAIL,
  registerViaUi,
  uniqueEmail,
} from "../support/helpers";

/**
 * Chat del asistente de reservas (Trozo 6b), de punta a punta contra el stack real, con un
 * LLM simulado (`e2e/fake-llm`, ver `docker-compose.e2e.yml`): abrir el chat desde /horario,
 * pedir una reserva, confirmarla, y verla de verdad en "Mis reservas". El LLM simulado no
 * entiende lenguaje natural: sigue el guion listar_clases -> reservar_clase -> proponer,
 * así que el mensaje al asistente incluye el nombre exacto de la clase de prueba.
 */
test("el socio reserva una clase hablando con el asistente y confirmando la propuesta", async ({
  page,
  request,
}) => {
  const ownerToken = await apiLogin(request, OWNER_EMAIL, DEMO_PASSWORD);
  const session = await createTestSession(request, ownerToken);

  await registerViaUi(page, {
    email: uniqueEmail("e2e.asistente"),
    password: DEMO_PASSWORD,
    fullName: "Socia Asistente E2E",
  });
  await expect(page).toHaveURL(/\/cuenta$/);

  await page.goto("/horario");
  await page.getByRole("button", { name: "Reservar con IA" }).click();

  const dialog = page.getByRole("dialog", { name: "Asistente de reservas" });
  await expect(dialog).toBeVisible();

  await dialog.getByLabel("Escribe tu mensaje").fill(`Resérvame la clase ${session.name}`);
  await dialog.getByRole("button", { name: "Enviar" }).click();

  await expect(dialog.getByText(session.name, { exact: false })).toBeVisible({ timeout: 15_000 });
  await dialog.getByRole("button", { name: "Confirmar" }).click();
  await expect(dialog.getByText(/reserva confirmada/i)).toBeVisible({ timeout: 15_000 });

  await dialog.getByRole("button", { name: "Cerrar el asistente" }).click();
  await expect(dialog).toBeHidden();

  await page.goto("/cuenta/reservas");
  await expect(page.getByText(session.name)).toBeVisible();
});
