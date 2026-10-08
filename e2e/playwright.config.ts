import { defineConfig, devices } from "@playwright/test";

/**
 * Tests end-to-end contra el stack real (frontend + backend + Postgres) en Docker.
 * No hay webServer aquí a propósito: `docker compose up` ya deja todo levantado con
 * los datos de ejemplo del seed; estos tests solo lo usan, no lo arrancan.
 */
export default defineConfig({
  testDir: "./tests",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? [["github"], ["html", { open: "never" }]] : "list",
  timeout: 30_000,
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:3000",
    trace: "retain-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
