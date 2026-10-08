import { expect, test } from "@playwright/test";
import {
  apiLogin,
  createTestSession,
  DEMO_PASSWORD,
  goToScheduleDay,
  OWNER_EMAIL,
  registerViaUi,
  uniqueEmail,
} from "../support/helpers";

/**
 * Reservar y cancelar una clase (LOGIC.md §5) desde /horario, con un socio nuevo (sin
 * reservas previas del seed) y una clase creada solo para este test, para no depender del
 * estado de ejemplo ni de que haya hueco libre en una sesión ya existente.
 */
test("un socio reserva una clase y la cancela", async ({ page, request }) => {
  const ownerToken = await apiLogin(request, OWNER_EMAIL, DEMO_PASSWORD);
  const session = await createTestSession(request, ownerToken);

  await registerViaUi(page, {
    email: uniqueEmail("e2e.socio"),
    password: DEMO_PASSWORD,
    fullName: "Socia E2E",
  });
  await expect(page).toHaveURL(/\/cuenta$/);

  await goToScheduleDay(page, session.startsAt);
  const card = page.getByTestId(`session-${session.id}`);
  await expect(card).toContainText(session.name);

  await card.getByRole("button", { name: "Reservar" }).click();
  await expect(card.getByText("Reservado", { exact: true })).toBeVisible();

  await card.getByRole("button", { name: "Cancelar" }).click();
  await expect(card.getByRole("button", { name: "Reservar" })).toBeVisible();
});
