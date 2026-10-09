import { expect, test } from "@playwright/test";
import { DEMO_PASSWORD, registerViaUi, uniqueEmail } from "../support/helpers";

/**
 * Cambio de tarifa (Trozo 9b, LOGIC.md §12): bajar de tarifa nunca es inmediato, se programa
 * para la siguiente renovación y se puede deshacer mientras siga pendiente. Un socio con la
 * tarifa más alta (Atleta Pro) programa una bajada a Básica y la deshace.
 */
test("un socio con tarifa Atleta Pro programa una bajada y la deshace", async ({ page }) => {
  await registerViaUi(page, {
    email: uniqueEmail("e2e.baja.tarifa"),
    password: DEMO_PASSWORD,
    fullName: "Socia Baja Tarifa E2E",
    planCode: "ATLETA_PRO",
  });
  await expect(page).toHaveURL(/\/cuenta$/);

  await page.goto("/cuenta/cuota");
  await expect(page.getByText("Tarifa Atleta Pro")).toBeVisible();
  await page.getByRole("link", { name: "Cambiar de tarifa" }).click();

  await expect(page).toHaveURL(/\/cuenta\/cuota\/cambiar-tarifa$/);
  await page
    .getByTestId("plan-option-BASICA")
    .getByRole("button", { name: "Cambiar en la próxima renovación" })
    .click();
  await page.getByRole("dialog", { name: "Confirmar cambio de tarifa" }).getByRole("button", { name: "Confirmar" }).click();

  await expect(page).toHaveURL(/\/cuenta\/cuota$/);
  await expect(page.getByText("Tienes programado un cambio a", { exact: false })).toBeVisible();
  await expect(page.getByText("Tarifa Básica", { exact: false })).toBeVisible();
  // Bajar nunca es inmediato: la tarifa activa sigue siendo la de antes hasta la renovación.
  await expect(page.getByText("Tarifa Atleta Pro")).toBeVisible();

  await page.getByRole("button", { name: "Deshacer" }).click();
  await expect(page.getByText("Tienes programado un cambio a", { exact: false })).not.toBeVisible();
});
