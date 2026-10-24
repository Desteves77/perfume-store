import { expect, test } from "@playwright/test";

test("interface pública permanece navegável no mobile e acessível", async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 });
  await page.goto("/");
  await page.getByRole("button", { name: "Abrir menu" }).click();
  await expect(page.getByRole("navigation", { name: "Principal" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Catálogo", exact: true })).toBeVisible();

  await page.goto("/catalog");
  await expect(page.getByLabel("Buscar")).toBeVisible();
  await expect(page.getByLabel("Categoria")).toBeVisible();
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1);
  expect(overflow).toBe(true);
  const missingAlt = await page.locator("img").evaluateAll((images) => images.some((image) => !image.getAttribute("alt")));
  expect(missingAlt).toBe(false);
});
