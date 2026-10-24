import { expect, test, type Dialog, type Page } from "@playwright/test";

test.describe.configure({ mode: "serial" });

const runId = `${Date.now()}-${Math.floor(Math.random() * 10000)}`;
const adminEmail = "admin.phase13@example.test";
const adminPassword = "Phase13Admin!2026";
const customerEmail = `customer.${runId}@example.test`;
const customerPassword = "CustomerPhase13!2026";
const productSlug = `perfume-phase13-${runId}`;
const productName = `Perfume Sintético ${runId}`;

async function login(page: Page, email: string, password: string) {
  await page.goto("/login");
  await page.getByLabel("E-mail").fill(email);
  await page.getByLabel("Senha").fill(password);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/\/account(?:$|\?)/);
}

async function browserPost(page: Page, path: string, body: unknown) {
  return page.evaluate(async ({ path, body }) => {
    const csrf = await fetch("/api/v1/security/csrf", { credentials: "include" }).then((response) => response.json());
    const response = await fetch(path, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": csrf.token },
      body: JSON.stringify(body),
    });
    return { status: response.status, body: await response.json().catch(() => null) };
  }, { path, body });
}

test("homologação integrada do cliente, pagamento e administração", async ({ page, browser }) => {
  test.setTimeout(120_000);
  await page.setViewportSize({ width: 1440, height: 1000 });

  await page.goto("/");
  await expect(page.getByRole("link", { name: "Catálogo", exact: true })).toBeVisible();
  await page.goto("/catalog");
  await expect(page.getByRole("heading", { name: "Encontre sua assinatura" })).toBeVisible();

  await login(page, adminEmail, adminPassword);
  await page.goto("/admin/catalog");
  await expect(page.getByRole("heading", { name: "Catálogo" })).toBeVisible();

  await page.getByLabel("Nome", { exact: true }).first().fill(`Categoria ${runId}`);
  await page.getByLabel("Slug", { exact: true }).first().fill(`categoria-${runId}`);
  await page.getByRole("button", { name: "Criar categoria" }).click();
  await expect(page.getByText("Categoria criada.")).toBeVisible();

  await page.getByLabel("Nome", { exact: true }).nth(1).fill(productName);
  await page.getByLabel("Marca", { exact: true }).fill("Marca Sintética");
  await page.locator("#slug").fill(productSlug);
  await page.getByLabel("Descrição", { exact: true }).fill("Fixture isolada para homologação do frontend.");
  await page.getByRole("button", { name: "Criar produto" }).click();
  await expect(page.getByText("Produto criado.")).toBeVisible();

  const productCard = page.getByTestId(`admin-product-${productSlug}`);
  await expect(productCard).toBeVisible();
  const prompts = [`SKU-${runId}`, "50", "129.90"];
  const dialogHandler = async (dialog: Dialog) => dialog.accept(prompts.shift() ?? "");
  page.on("dialog", dialogHandler);
  await productCard.getByRole("button", { name: "Adicionar apresentação" }).click();
  await expect(page.getByText("Apresentação criada.")).toBeVisible();
  page.off("dialog", dialogHandler);

  const png = Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=", "base64");
  await productCard.locator('input[type="file"]').setInputFiles({ name: "phase13.png", mimeType: "image/png", buffer: png });
  await expect(page.getByText("Imagem enviada.")).toBeVisible();
  await productCard.getByRole("button", { name: "Publicar" }).click();
  await expect(page.getByText("Produto publicado.")).toBeVisible();

  const presentationId = await productCard.getByTestId("presentation-id").first().innerText();
  const presentation = presentationId.replace("ID: ", "").trim();
  await page.goto("/admin/inventory");
  await page.getByLabel("ID da apresentação").fill(presentation);
  await page.getByLabel("Quantidade ou delta").fill("5");
  await page.getByLabel("Motivo").fill("Estoque sintético da homologação");
  await page.getByRole("button", { name: "Salvar ajuste" }).click();
  await expect(page.getByText("Ajuste registrado.")).toBeVisible();

  await page.getByRole("button", { name: "Sair" }).click();
  await expect(page.getByRole("navigation", { name: "Principal" }).getByRole("link", { name: "Entrar" })).toBeVisible();

  await page.goto(`/catalog/${productSlug}`);
  await expect(page.getByRole("heading", { name: productName })).toBeVisible();
  await page.getByRole("button", { name: "Adicionar ao carrinho" }).click();
  await expect(page.getByText("Apresentação adicionada ao carrinho.")).toBeVisible();
  await page.goto("/cart");
  await expect(page.getByText(productName)).toBeVisible();
  await page.reload();
  await expect(page.getByText(productName)).toBeVisible();

  await page.goto("/register");
  await page.getByLabel("Nome").fill(`Cliente ${runId}`);
  await page.getByLabel("E-mail").fill(customerEmail);
  await page.getByLabel("Senha").fill(customerPassword);
  await page.getByRole("button", { name: "Criar conta" }).click();
  await page.waitForURL(/\/login/, { timeout: 15_000 });

  await login(page, customerEmail, customerPassword);
  await page.goto("/cart");
  await expect(page.getByText(productName)).toBeVisible();

  await page.goto("/account/addresses");
  await page.getByLabel("Destinatário").fill(`Cliente ${runId}`);
  await page.getByLabel("Identificação").fill("Casa");
  await page.getByLabel("CEP").fill("01311000");
  await page.getByLabel("UF").fill("SP");
  await page.getByLabel("Logradouro").fill("Avenida Paulista");
  await page.getByLabel("Número").fill("1000");
  await page.getByLabel("Bairro").fill("Bela Vista");
  await page.getByLabel("Cidade").fill("São Paulo");
  await page.getByRole("button", { name: "Salvar" }).click();
  await expect(page.getByText("Endereço cadastrado.")).toBeVisible();
  await expect(page.getByText("Avenida Paulista")).toBeVisible();
  await page.reload();
  await expect(page.getByText("Avenida Paulista")).toBeVisible();

  await page.goto("/checkout");
  await page.getByRole("button", { name: "Calcular frete" }).click();
  await expect(page.getByText(/dias/)).toBeVisible();
  await page.getByRole("button", { name: "Criar pedido" }).click();
  await page.waitForURL(/\/account\/orders\//);
  const publicNumber = new URL(page.url()).pathname.split("/").pop() ?? "";
  await expect(page.getByRole("heading", { name: publicNumber })).toBeVisible();
  await expect(page.getByText("Pagamento pendente", { exact: true }).first()).toBeVisible();

  const missingCsrf = await page.evaluate(async () => {
    const response = await fetch("/api/v1/cart/items", { method: "POST", credentials: "include", headers: { "Content-Type": "application/json" }, body: "{}" });
    return response.status;
  });
  expect(missingCsrf).toBe(403);

  const approval = await browserPost(page, `/api/v1/dev/payment/orders/${publicNumber}`, { outcome: "APPROVED" });
  expect(approval.status).toBe(200);
  await page.reload();
  await expect(page.getByText("Confirmado", { exact: true }).first()).toBeVisible();

  await page.getByRole("button", { name: "Sair" }).click();
  await page.goto("/admin");
  await expect(page.getByRole("heading", { name: "Acesso restrito" })).toBeVisible();
  await login(page, adminEmail, adminPassword);
  await page.goto("/admin");
  await expect(page.getByText(publicNumber)).toBeVisible();
  await page.getByRole("link", { name: publicNumber }).click();
  await expect(page.getByRole("heading", { name: publicNumber })).toBeVisible();
  await page.getByRole("button", { name: "Iniciar separação" }).click();
  await expect(page.getByText("Em separação", { exact: true })).toBeVisible();
  await page.getByLabel("Transportadora").fill("Transportadora Sintética");
  await page.getByLabel("Rastreamento").fill(`TRACK-${runId}`);
  await page.getByRole("button", { name: "Registrar envio" }).click();
  await expect(page.getByText("Enviado", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Confirmar entrega" }).click();
  await expect(page.getByText("Entregue", { exact: true })).toBeVisible();

  await page.getByLabel("Motivo", { exact: true }).last().fill("Homologação administrativa");
  await page.getByRole("button", { name: "Solicitar reembolso" }).click();
  await expect(page.getByText(/PENDING_PROVIDER|pendente/i)).toBeVisible();

  await page.goto("/admin/notifications");
  await expect(page.getByRole("heading", { name: "Notificações" })).toBeVisible();
  await page.goto("/admin");
  await page.getByRole("link", { name: publicNumber }).click();
  await page.getByRole("button", { name: "Registrar devolução" }).first().click();
  await expect(page.getByText("Solicitada", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Registrar recebimento" }).click();
  await expect(page.getByText("Aguardando inspeção", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Registrar inspeção" }).click();
  await expect(page.getByText("Inspecionada", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Repor estoque" }).click();
  await expect(page.getByText("Reposta", { exact: true })).toBeVisible();

  const customerB = await browser.newPage();
  const customerBEmail = `customer-b.${runId}@example.test`;
  await customerB.goto("/register");
  await customerB.getByLabel("Nome").fill(`Cliente B ${runId}`);
  await customerB.getByLabel("E-mail").fill(customerBEmail);
  await customerB.getByLabel("Senha").fill(customerPassword);
  await customerB.getByRole("button", { name: "Criar conta" }).click();
  await customerB.waitForURL(/\/login/, { timeout: 5_000 });
  await login(customerB, customerBEmail, customerPassword);
  await customerB.goto(`/account/orders/${publicNumber}`);
  await expect(customerB.getByRole("alert")).toBeVisible();
  await customerB.close();
});
