import { expect, test } from "@playwright/test";
import {
  apiLogin,
  createTestSession,
  DEMO_PASSWORD,
  DEV_ADMIN_EMAIL,
  DEV_ADMIN_PASSWORD,
  loginViaUi,
  OWNER_EMAIL,
  registerViaUi,
  uniqueEmail,
} from "../support/helpers";

/**
 * Tarjetas reales en el chat del asistente y "Cambiar horario" (Trozo 9c, commit 2): el seed de
 * dev crea una clase de CrossFit todos los días a las 07:00 ("CrossFit Metcon"), así que junto a
 * la clase de prueba que crea este test (CrossFit, mañana) siempre hay al menos dos candidatas
 * reales para la misma disciplina y ventana de hoy/mañana: justo lo que hace falta para que
 * `options` tenga más de una y aparezca "Cambiar horario". El LLM simulado (`e2e/fake-llm`)
 * siempre pide `listar_clases` filtrando por CrossFit (ver server.js) y propone la clase cuyo
 * nombre aparece tal cual en el primer mensaje del socio.
 */
test.describe("Chat del asistente en móvil", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test("la propuesta lleva datos reales y 'Cambiar horario' permite reservar otra clase", async ({
    page,
    request,
  }) => {
    const ownerToken = await apiLogin(request, OWNER_EMAIL, DEMO_PASSWORD);
    const session = await createTestSession(request, ownerToken, { daysAhead: 1 });

    await registerViaUi(page, {
      email: uniqueEmail("e2e.tarjetas"),
      password: DEMO_PASSWORD,
      fullName: "Socia Tarjetas E2E",
    });
    await expect(page).toHaveURL(/\/cuenta$/);

    await page.goto("/horario");
    await page.getByRole("button", { name: "Reservar con IA" }).click();
    const dialog = page.getByRole("dialog", { name: "Asistente de reservas" });
    await expect(dialog).toBeVisible();

    await dialog.getByLabel("Escribe tu mensaje").fill(`Resérvame la clase ${session.name}`);
    await dialog.getByRole("button", { name: "Enviar" }).click();

    // Tarjeta de propuesta con los datos reales de la clase (no solo una frase del modelo). El
    // nombre de la clase no sirve para localizarla sin ambigüedad: el mensaje del socio, que ya
    // lo incluye, también hace match. "Confirmar" y la sala solo existen en la tarjeta.
    await expect(dialog.getByRole("button", { name: "Confirmar" })).toBeVisible({ timeout: 15_000 });
    await expect(dialog.getByText("Sala E2E")).toBeVisible();
    await expect(dialog.getByText(/plazas libres/)).toBeVisible();

    // Hay otra clase real de CrossFit (la del seed, "CrossFit Metcon"): debe poder cambiarse.
    const changeButton = dialog.getByRole("button", { name: "Cambiar horario" });
    await expect(changeButton).toBeVisible();
    await changeButton.click();

    const alternatives = dialog.getByRole("group", { name: "Otros horarios disponibles" });
    await expect(alternatives).toBeVisible();
    // El seed crea "CrossFit Metcon" todos los días: puede haber una para hoy y otra para
    // mañana, además de la clase de prueba: basta con elegir cualquiera de las del seed.
    const seededOption = alternatives.getByRole("button", { name: /CrossFit Metcon/ }).first();
    await expect(seededOption).toBeVisible();
    await seededOption.click();

    // Al elegir la alternativa, la propuesta pasa a ser la clase del seed, no la de prueba.
    await expect(dialog.getByText("CrossFit Metcon", { exact: false }).first()).toBeVisible();

    await dialog.getByRole("button", { name: "Confirmar" }).click();
    await expect(dialog.getByText(/reserva confirmada/i)).toBeVisible({ timeout: 15_000 });

    await page.goto("/cuenta/reservas");
    await expect(page.getByText("CrossFit Metcon")).toBeVisible();
    await expect(page.getByText(session.name)).toBeHidden();
  });
});

/**
 * Panel de gestión en móvil (Trozo 9c, commit 2): pestañas en una sola fila con desplazamiento
 * horizontal (en vez de partirse en varias líneas) y el nombre/rol/"Salir" dentro del menú del
 * avatar, no en una fila propia.
 */
test.describe("Panel de gestión en móvil", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test("las pestañas van en una sola fila, sin partirse en varias líneas", async ({ page }) => {
    await loginViaUi(page, OWNER_EMAIL, DEMO_PASSWORD);
    await expect(page).toHaveURL(/\/admin$/);

    const nav = page.getByRole("navigation", { name: "Navegación del panel de gestión" });
    await expect(nav).toBeVisible();
    const links = await nav.getByRole("link").all();
    expect(links.length).toBe(5);

    const tops = await Promise.all(
      links.map(async (link) => {
        const box = await link.boundingBox();
        return box?.y;
      }),
    );
    expect(new Set(tops).size).toBe(1); // mismo "y" para las 5 pestañas: una sola fila
  });

  test("el nombre, el rol y 'Salir' viven en el menú del avatar, no en una fila propia", async ({ page }) => {
    await loginViaUi(page, OWNER_EMAIL, DEMO_PASSWORD);
    await expect(page).toHaveURL(/\/admin$/);

    const menu = page.getByRole("menu");
    await expect(menu).toBeHidden();

    const avatarButton = page.getByRole("button", { name: /Cuenta de Javier M\./ });
    await avatarButton.click();
    await expect(menu.getByText("Dueño", { exact: true })).toBeVisible();
    await expect(menu.getByRole("menuitem", { name: "Salir" })).toBeVisible();
  });

  test("el avatar saca las iniciales ignorando lo que va entre paréntesis", async ({ page }) => {
    await loginViaUi(page, DEV_ADMIN_EMAIL, DEV_ADMIN_PASSWORD);
    await expect(page).toHaveURL(/\/admin$/);

    const avatarButton = page.getByRole("button", { name: /Cuenta de/ });
    await expect(avatarButton).toBeVisible();
    const text = (await avatarButton.textContent())?.trim() ?? "";
    expect(text).not.toContain("(");
    expect(text.length).toBeLessThanOrEqual(2);
  });
});

test.describe("Panel de gestión en escritorio", () => {
  test("no cambia: pestañas y cuenta siguen en la misma fila, sin menú de avatar", async ({ page }) => {
    await loginViaUi(page, OWNER_EMAIL, DEMO_PASSWORD);
    await expect(page).toHaveURL(/\/admin$/);

    await expect(page.getByRole("button", { name: /Cuenta de/ })).toBeHidden();
    await expect(page.getByText("Javier M.")).toBeVisible();
    await expect(page.getByText("Dueño", { exact: true })).toBeVisible();
  });
});
