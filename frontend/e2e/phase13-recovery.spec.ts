import { expect, test } from "@playwright/test";

test("recuperação de senha entrega e usa o token pelo navegador", async ({ page, request }) => {
  const runId = `${Date.now()}-${Math.floor(Math.random() * 10000)}`;
  const email = `recovery.${runId}@example.test`;
  const originalPassword = "RecoveryOriginal!2026";
  const newPassword = "RecoveryUpdated!2026";

  await page.goto("/register");
  await page.getByLabel("Nome").fill(`Recovery ${runId}`);
  await page.getByLabel("E-mail").fill(email);
  await page.getByLabel("Senha").fill(originalPassword);
  await page.getByRole("button", { name: "Criar conta" }).click();
  await expect(page).toHaveURL(/\/login$/);

  await page.goto("/recover");
  await page.getByLabel("E-mail").fill(email);
  await page.getByRole("button", { name: "Enviar instruções" }).click();
  await expect(page.getByText(/Se o e-mail estiver cadastrado/)).toBeVisible();

  const findMessageId = async () => {
    const response = await request.get("http://localhost:18635/api/v1/messages?limit=100");
    const payload = await response.json();
    const message = payload.messages.find((item: { To?: Array<{ Address: string }>; Subject?: string }) =>
      item.To?.some((recipient) => recipient.Address === email) && /senha|acesso/i.test(item.Subject ?? ""),
    );
    return message?.ID ?? "";
  };
  await expect.poll(findMessageId, { timeout: 15000 }).not.toBe("");
  const messageId = await findMessageId();

  const detailResponse = await request.get(`http://localhost:18635/api/v1/message/${messageId}`);
  const detail = await detailResponse.json();
  const tokenMatch = String(detail.Text).match(/https:\/\/localhost:19680\/reset-password\?token=([^\s]+)/);
  expect(tokenMatch).not.toBeNull();
  const token = tokenMatch?.[1] ?? "";

  await page.goto(`https://localhost:19680/reset-password?token=${token}`);
  await expect(page.getByLabel("Token do e-mail")).toHaveValue(token);
  await page.getByLabel("Nova senha").fill(newPassword);
  await page.getByRole("button", { name: "Redefinir senha" }).click();
  await expect(page.getByText("Senha redefinida. Você já pode entrar.")).toBeVisible();

  await page.goto("/login");
  await page.getByLabel("E-mail").fill(email);
  await page.getByLabel("Senha").fill(newPassword);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/\/account$/);
  await expect(page.locator("h1")).toBeVisible();
});
