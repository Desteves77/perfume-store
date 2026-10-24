import { test, expect } from "@playwright/test";

test("catálogo público apresenta a navegação principal", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("link", { name: "Catálogo", exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: /Carrinho/ }).first()).toBeVisible();
});

test("frontend consegue consultar o catálogo público do backend", async ({ page, request }) => {
  const apiUrl = (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080").replace(/\/$/, "");
  const response = await request.get(apiUrl + "/api/v1/catalog/products?page=0&size=12&sort=name,asc", { ignoreHTTPSErrors: true });
  expect(response.ok()).toBeTruthy();
  await page.goto("/catalog");
  await expect(page.getByRole("heading", { name: "Encontre sua assinatura" })).toBeVisible();
});
