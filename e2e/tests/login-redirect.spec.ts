import { expect, test } from "@playwright/test";
import { DEMO_PASSWORD, loginViaUi, MEMBER_EMAIL, OWNER_EMAIL, STAFF_EMAIL } from "../support/helpers";

/**
 * Un único login redirige por rol (LOGIC.md §2 y §6): el socio va a /cuenta y nunca puede
 * entrar en /admin (la guarda del cliente lo manda de vuelta); dueño y staff van a /admin.
 * Cuentas del seed de demo, solo lectura: no hace falta ningún dato nuevo para este test.
 */
test.describe("Login por rol", () => {
  test("un socio entra en /cuenta y no puede entrar en /admin", async ({ page }) => {
    await loginViaUi(page, MEMBER_EMAIL, DEMO_PASSWORD);
    await expect(page).toHaveURL(/\/cuenta$/);

    await page.goto("/admin");
    await expect(page).toHaveURL(/\/cuenta$/);
  });

  test("el dueño entra directamente en /admin", async ({ page }) => {
    await loginViaUi(page, OWNER_EMAIL, DEMO_PASSWORD);
    await expect(page).toHaveURL(/\/admin$/);
  });

  test("el staff entra directamente en /admin", async ({ page }) => {
    await loginViaUi(page, STAFF_EMAIL, DEMO_PASSWORD);
    await expect(page).toHaveURL(/\/admin$/);
  });
});
