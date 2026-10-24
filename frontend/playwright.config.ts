import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  timeout: 30_000,
  use: {
    baseURL: process.env.FRONTEND_E2E_URL ?? "http://localhost:3000",
    ignoreHTTPSErrors: true,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: process.env.CI ? undefined : { command: "npm run dev", url: "http://localhost:3000", reuseExistingServer: true },
});
