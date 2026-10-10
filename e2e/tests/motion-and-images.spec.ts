import { expect, test } from "@playwright/test";

/**
 * Movimiento de la portada (Trozo 7b, DESIGN.md §6): con `prefers-reduced-motion` activo no
 * debe haber nada que se mueva solo ni contenido que dependa de hacer scroll para verse.
 */
test.describe("Movimiento con prefers-reduced-motion", () => {
  test.use({ reducedMotion: "reduce" });

  test("las secciones con aparición al hacer scroll ya se ven sin necesidad de bajar", async ({ page }) => {
    await page.goto("/");

    // "Nuestro hierro" vive varias pantallas más abajo: con reduced-motion debe estar visible
    // (opacidad 1) ya en el primer render, sin esperar a que el observador de scroll dispare.
    const section = page.getByRole("heading", { name: "Nuestro hierro" }).locator("..");
    await expect(section).toBeVisible();
    await expect(section).toHaveCSS("opacity", "1");
  });

  test("el titular del hero no cambia de palabra solo", async ({ page }) => {
    await page.goto("/");
    const heading = page.getByRole("heading", { level: 1 });
    await expect(heading).toContainText("OLIMPO");

    await page.waitForTimeout(3500); // más que el intervalo de 3 s del cambio de palabra
    await expect(heading).toContainText("OLIMPO");
  });
});

test.describe("Imágenes de la portada", () => {
  test("el hero y las fotos de contenido tienen el texto alternativo esperado", async ({ page }) => {
    await page.goto("/");

    // Decorativa: el titular ya dice lo mismo, así que el alt vacío es correcto (no redundante
    // para quien usa un lector de pantalla).
    const heroImg = page.locator("img[alt='']").first();
    await expect(heroImg).toBeAttached();

    await expect(page.getByAltText("Zona de peso libre con mancuernas ordenadas en el gimnasio")).toBeVisible();
    await expect(page.getByAltText("Socias entrenando en una clase dirigida")).toBeVisible();
  });
});
