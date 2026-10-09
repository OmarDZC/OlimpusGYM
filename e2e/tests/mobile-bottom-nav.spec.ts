import { expect, test } from "@playwright/test";
import { DEMO_PASSWORD, registerViaUi, uniqueEmail } from "../support/helpers";

/**
 * Arreglo 2: la barra inferior de móvil solo aparecía en /cuenta (Inicio) porque /horario
 * ("Reservar" en la propia barra) vive fuera del layout de /cuenta y no la heredaba. En móvil
 * debe verse en todas las pantallas del socio; en escritorio, nunca (se queda el menú superior).
 */
const MEMBER_ROUTES = ["/cuenta", "/horario", "/entrenador-ia", "/cuenta/reservas", "/cuenta/entrenamiento", "/cuenta/cuota", "/cuenta/perfil"];

test.describe("Barra inferior en móvil", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test("se ve en todas las pantallas del socio", async ({ page }) => {
    await registerViaUi(page, {
      email: uniqueEmail("e2e.barra-movil"),
      password: DEMO_PASSWORD,
      fullName: "Socia Barra Móvil",
    });
    await expect(page).toHaveURL(/\/cuenta$/);

    for (const route of MEMBER_ROUTES) {
      await page.goto(route);
      await expect(page.getByRole("navigation", { name: "Navegación de tu cuenta" })).toBeVisible();
    }
  });

  test("marca la pestaña activa según la ruta", async ({ page }) => {
    await registerViaUi(page, {
      email: uniqueEmail("e2e.barra-activa"),
      password: DEMO_PASSWORD,
      fullName: "Socia Barra Activa",
    });
    await expect(page).toHaveURL(/\/cuenta$/);

    await page.goto("/horario");
    const nav = page.getByRole("navigation", { name: "Navegación de tu cuenta" });
    await expect(nav.getByRole("link", { name: "Reservar" })).toHaveAttribute("aria-current", "page");
    await expect(nav.getByRole("link", { name: "Inicio" })).not.toHaveAttribute("aria-current", "page");
  });
});

test.describe("Barra inferior en escritorio", () => {
  test("no aparece: se mantiene el menú superior", async ({ page }) => {
    await registerViaUi(page, {
      email: uniqueEmail("e2e.sin-barra-escritorio"),
      password: DEMO_PASSWORD,
      fullName: "Socia Escritorio",
    });
    await expect(page).toHaveURL(/\/cuenta$/);

    await page.goto("/horario");
    await expect(page.getByRole("navigation", { name: "Navegación de tu cuenta" })).toBeHidden();
    // "Horario" también aparece en las pestañas Horario/Mis reservas (Bloque 2): se acota al
    // menú superior para no chocar con ese otro enlace, igual de válido, al mismo texto.
    await expect(page.getByRole("navigation", { name: "Navegación principal" }).getByRole("link", { name: "Horario" })).toBeVisible();
  });
});
