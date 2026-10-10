import { expect, test } from "@playwright/test";
import {
  API_URL,
  DEMO_PASSWORD,
  loginViaUi,
  MEMBER_EMAIL,
  OWNER_EMAIL,
  uniqueEmail,
  uniqueName,
} from "../support/helpers";

/**
 * Franja "En directo" (Trozo 7b, LOGIC.md §9) y mensajes de contacto en el panel de gestión.
 */
test.describe("Franja En directo", () => {
  test("muestra datos reales de GET /public/live en la portada", async ({ page, request }) => {
    const liveResponse = await request.get(`${API_URL}/api/v1/public/live`);
    expect(liveResponse.ok()).toBeTruthy();
    const live = (await liveResponse.json()) as { classesRemainingToday: number };

    await page.goto("/");
    const strip = page.getByText("En directo").locator("..");
    await expect(strip).toBeVisible();

    if (live.classesRemainingToday === 0) {
      await expect(strip).toContainText("No quedan clases por empezar hoy");
    } else {
      await expect(strip).toContainText(`${live.classesRemainingToday}`);
    }
  });
});

test.describe("Mensajes en el panel de gestión", () => {
  test("el dueño ve un mensaje enviado desde /contacto, y un socio no puede verlos", async ({ page, request }) => {
    const messageText = uniqueName("Mensaje e2e");

    await page.goto("/contacto");
    await page.getByLabel("Nombre").fill(uniqueName("Visitante Mensajes"));
    await page.getByLabel("Correo electrónico").fill(uniqueEmail("visitante.mensajes"));
    await page.getByLabel("Mensaje").fill(messageText);
    await page.getByRole("button", { name: "Enviar mensaje" }).click();
    await expect(page.getByText("Mensaje enviado. Te responderemos en cuanto podamos.")).toBeVisible();

    await loginViaUi(page, OWNER_EMAIL, DEMO_PASSWORD);
    await expect(page).toHaveURL(/\/admin$/);
    await page.goto("/admin/mensajes");
    await expect(page.getByText(messageText)).toBeVisible();

    // Un socio no llega ni a pedir la lista: la guarda del panel lo manda de vuelta a /cuenta.
    const memberToken = await request
      .post(`${API_URL}/api/v1/auth/login`, { data: { email: MEMBER_EMAIL, password: DEMO_PASSWORD } })
      .then((r) => r.json())
      .then((body) => body.token as string);
    const forbidden = await request.get(`${API_URL}/api/v1/admin/messages`, {
      headers: { Authorization: `Bearer ${memberToken}` },
    });
    expect(forbidden.status()).toBe(403);
  });
});
