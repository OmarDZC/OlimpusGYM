import { expect, test } from "@playwright/test";
import { DEMO_PASSWORD, registerViaUi, uniqueEmail } from "../support/helpers";

/**
 * Bloque 2: en el área del socio, la barra inferior ya hace de menú en móvil, así que la
 * hamburguesa de la cabecera (pensada para la web pública) sobra ahí y confundía (dos menús
 * para lo mismo). Y como "Mis reservas" no es una de las 5 pestañas de la barra, hacían falta
 * las pestañas "Horario"/"Mis reservas" dentro de la propia pantalla de Reservar para poder
 * llegar a él en móvil.
 */
const MEMBER_ROUTES = ["/cuenta", "/horario", "/entrenador-ia", "/cuenta/reservas", "/cuenta/entrenamiento", "/cuenta/cuota", "/cuenta/perfil"];

test.describe("Menú móvil del área de socio", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test("no hay menú de tres rayas en ninguna ruta del socio, y la barra inferior se ve en todas", async ({ page }) => {
    await registerViaUi(page, {
      email: uniqueEmail("e2e.sin-hamburguesa"),
      password: DEMO_PASSWORD,
      fullName: "Socia Sin Hamburguesa",
    });
    await expect(page).toHaveURL(/\/cuenta$/);

    for (const route of MEMBER_ROUTES) {
      await page.goto(route);
      await expect(page.getByRole("button", { name: /Abrir menú|Cerrar menú/ })).toHaveCount(0);
      await expect(page.getByRole("navigation", { name: "Navegación de tu cuenta" })).toBeVisible();
    }
  });

  test("el menú de tres rayas sigue existiendo fuera del área de socio", async ({ page }) => {
    await registerViaUi(page, {
      email: uniqueEmail("e2e.con-hamburguesa"),
      password: DEMO_PASSWORD,
      fullName: "Socia Con Hamburguesa",
    });
    await expect(page).toHaveURL(/\/cuenta$/);

    await page.goto("/");
    await expect(page.getByRole("button", { name: "Abrir menú" })).toBeVisible();
  });

  test("las pestañas Horario / Mis reservas enlazan entre las dos pantallas", async ({ page }) => {
    await registerViaUi(page, {
      email: uniqueEmail("e2e.pestanas-reservar"),
      password: DEMO_PASSWORD,
      fullName: "Socia Pestañas",
    });
    await expect(page).toHaveURL(/\/cuenta$/);

    await page.goto("/horario");
    const tabs = page.getByRole("navigation", { name: "Reservar" });
    await expect(tabs.getByRole("link", { name: "Horario" })).toHaveAttribute("aria-current", "page");

    await tabs.getByRole("link", { name: "Mis reservas" }).click();
    await expect(page).toHaveURL(/\/cuenta\/reservas$/);
    await expect(tabs.getByRole("link", { name: "Mis reservas" })).toHaveAttribute("aria-current", "page");

    await tabs.getByRole("link", { name: "Horario" }).click();
    await expect(page).toHaveURL(/\/horario$/);
  });
});

test.describe("Menú móvil del área de socio en escritorio", () => {
  test("no cambia nada: sigue el menú superior normal", async ({ page }) => {
    await registerViaUi(page, {
      email: uniqueEmail("e2e.escritorio-sin-cambios"),
      password: DEMO_PASSWORD,
      fullName: "Socia Escritorio",
    });
    await expect(page).toHaveURL(/\/cuenta$/);

    await page.goto("/horario");
    await expect(page.getByRole("navigation", { name: "Navegación de tu cuenta" })).toBeHidden();
    await expect(page.getByRole("link", { name: "Entrenador IA" })).toBeVisible();
  });
});
