import { expect, test } from "@playwright/test";
import { API_URL, uniqueEmail, uniqueName } from "../support/helpers";

type PublicPlan = { planCode: string; planName: string; priceCents: number };

function formatPrice(cents: number): string {
  return (cents / 100).toLocaleString("es-ES", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

/**
 * Web pública recortada (Trozo 7a): /, /tarifas y /contacto. Sin sesión, de punta a punta
 * contra el stack real.
 */
test.describe("Portada", () => {
  test("se ve, con los enlaces principales y las tarifas reales del backend", async ({ page, request }) => {
    const plansResponse = await request.get(`${API_URL}/api/v1/public/plans`);
    expect(plansResponse.ok()).toBeTruthy();
    const plans = (await plansResponse.json()) as PublicPlan[];
    expect(plans.length).toBe(3);

    await page.goto("/");
    await expect(page.getByRole("heading", { name: /Forja tu propio/i })).toBeVisible();

    await expect(page.getByRole("link", { name: "Ver horario", exact: true })).toHaveAttribute("href", "/horario");
    await expect(page.getByRole("link", { name: "Ver tarifas", exact: true })).toHaveAttribute("href", "/tarifas");

    for (const plan of plans) {
      await expect(page.getByText(plan.planName)).toBeVisible();
      await expect(page.getByText(`${formatPrice(plan.priceCents)} €`).first()).toBeVisible();
    }
  });
});

test.describe("Tarifas", () => {
  test("muestra las tres tarifas reales y enlaza a alta para quien no tiene sesión", async ({ page, request }) => {
    const plansResponse = await request.get(`${API_URL}/api/v1/public/plans`);
    const plans = (await plansResponse.json()) as PublicPlan[];

    await page.goto("/tarifas");
    await expect(page.getByRole("heading", { name: /Tarifas/i }).first()).toBeVisible();

    for (const plan of plans) {
      await expect(page.getByText(plan.planName).first()).toBeVisible();
    }

    const ctaButtons = page.getByRole("link", { name: "Hazte socio" });
    await expect(ctaButtons).toHaveCount(3);
    await expect(ctaButtons.first()).toHaveAttribute("href", "/acceso?modo=registro");

    // Comparación accesible: una tabla de verdad, no una maqueta visual con divs.
    const table = page.getByRole("table");
    await expect(table).toBeVisible();
    await expect(table.getByRole("row", { name: /Precio/ })).toBeVisible();
  });
});

// Serial a propósito: el límite de envíos por IP (Trozo 7a) es compartido por todos los tests
// de este bloque (misma IP de origen, el runner de Playwright). En paralelo competirían por el
// mismo cupo; en serie, el envío normal consume su hueco antes de que el test del límite agote
// el resto a propósito.
test.describe.serial("Contacto", () => {
  test("muestra la dirección, el horario y enlaza correctamente desde el menú", async ({ page }) => {
    await page.goto("/");
    await page.getByRole("navigation", { name: "Navegación principal" }).getByRole("link", { name: "Contacto" }).click();
    await expect(page).toHaveURL(/\/contacto$/);
    await expect(page.getByText("dirección de ejemplo").first()).toBeVisible();
    await expect(page.getByText(/Sala de pesas: 06:00 a 23:00/).first()).toBeVisible();
  });

  test("envía el formulario y muestra el mensaje de éxito", async ({ page }) => {
    await page.goto("/contacto");
    await page.getByLabel("Nombre").fill(uniqueName("Visitante"));
    await page.getByLabel("Correo electrónico").fill(uniqueEmail("visitante"));
    await page.getByLabel("Mensaje").fill("¿Tenéis clases de prueba gratuitas?");
    await page.getByRole("button", { name: "Enviar mensaje" }).click();

    await expect(page.getByText("Mensaje enviado. Te responderemos en cuanto podamos.")).toBeVisible();
  });

  test("el campo trampa rechaza el envío", async ({ request }) => {
    const response = await request.post(`${API_URL}/api/v1/public/contact`, {
      data: {
        name: "Bot",
        email: uniqueEmail("bot"),
        message: "Mensaje de spam automático",
        website: "http://spam.example",
      },
    });
    expect(response.status()).toBe(400);
  });

  test("responde 429 tras demasiados envíos seguidos desde el mismo origen", async ({ request }) => {
    let lastStatus = 0;
    for (let i = 0; i < 5; i++) {
      const response = await request.post(`${API_URL}/api/v1/public/contact`, {
        data: {
          name: uniqueName("Visitante Límite"),
          email: uniqueEmail("limite"),
          message: `Mensaje de prueba número ${i}`,
        },
      });
      lastStatus = response.status();
      if (lastStatus === 429) break;
    }
    expect(lastStatus).toBe(429);
  });
});
