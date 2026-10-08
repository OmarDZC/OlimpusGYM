import { expect, test } from "@playwright/test";
import {
  apiLogin,
  createTestSession,
  DEMO_PASSWORD,
  goToScheduleDay,
  loginViaUi,
  logout,
  OWNER_EMAIL,
  registerViaUi,
  STAFF_EMAIL,
  uniqueEmail,
} from "../support/helpers";

/**
 * Regla aplazada del Trozo 2, construida en el Trozo 4a (LOGIC.md §5.5): si staff cancela
 * una sesión completa, todas sus reservas activas pasan a CANCELLED. Se comprueba de punta
 * a punta: el socio reserva, el staff cancela la clase desde /admin/clases, y la reserva
 * desaparece de "Mis reservas" del socio (que solo lista BOOKED/WAITLIST).
 */
test("el staff cancela una sesión y la reserva del socio queda cancelada", async ({ page, request }) => {
  const ownerToken = await apiLogin(request, OWNER_EMAIL, DEMO_PASSWORD);
  const session = await createTestSession(request, ownerToken);
  const memberEmail = uniqueEmail("e2e.socio-cancel");

  await registerViaUi(page, { email: memberEmail, password: DEMO_PASSWORD, fullName: "Socio E2E Cancelación" });
  await expect(page).toHaveURL(/\/cuenta$/);

  await goToScheduleDay(page, session.startsAt);
  const scheduleCard = page.getByTestId(`session-${session.id}`);
  await scheduleCard.getByRole("button", { name: "Reservar" }).click();
  await expect(scheduleCard.getByText("Reservado", { exact: true })).toBeVisible();

  await logout(page);
  await loginViaUi(page, STAFF_EMAIL, DEMO_PASSWORD);
  await expect(page).toHaveURL(/\/admin$/);

  await page.goto("/admin/clases");
  const adminCard = page.getByTestId(`admin-session-${session.id}`);
  await expect(adminCard).toContainText(session.name);
  await adminCard.getByRole("button", { name: "Cancelar clase" }).click();
  await expect(adminCard).toHaveCount(0);

  await logout(page);
  await loginViaUi(page, memberEmail, DEMO_PASSWORD);
  await expect(page).toHaveURL(/\/cuenta$/);

  await page.goto("/cuenta/reservas");
  await expect(page.getByText(session.name)).toHaveCount(0);
});
