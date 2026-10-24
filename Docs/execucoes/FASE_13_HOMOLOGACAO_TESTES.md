# Fase 13 — continuação: testes e evidências

## Backend

Comando final: `java -version; .\mvnw.cmd -version; .\mvnw.cmd clean verify` em `backend/`.

- Java efetivo do Wrapper: 17.0.16.
- Maven Wrapper: 3.9.16.
- PostgreSQL real: Testcontainers PostgreSQL 18.6.
- 82 testes executados, 82 aprovados, 0 falhas, 0 erros, 0 ignorados.
- Classes cobertas: AddressShipping, AdminProvisioning, ApiContract, Authentication, Cart, Catalog, Infrastructure, Inventory, Notification, Order, Payment, PostSale, ProductionProfileIsolation e SessionPersistence.
- Flyway aplicou V1–V10 em bancos limpos de teste.
- JAR executável gerado em `backend/target/perfumestore-backend-0.0.1-SNAPSHOT.jar`.

Os avisos de conexão recusada ocorreram em schedulers de sessões durante o encerramento de instâncias Testcontainers já finalizadas. O Surefire terminou com todos os testes aprovados e o Maven terminou `BUILD SUCCESS`.

## Frontend

| Comando | Resultado |
|---|---|
| `npm run typecheck` | aprovado |
| `npm run lint` | aprovado |
| `npm test` | 1 arquivo, 2 testes aprovados |
| `npm run build` | aprovado; Next.js 15.5.28; 22 rotas |
| `npm run test:e2e` | aprovado; 5 testes, 5 aprovados |

## E2E integrado

`frontend/e2e/phase13-integration.spec.ts` executou em navegador real contra `https://localhost:19680`:

- criação e publicação de categoria/produto/apresentação/imagem pelo ADMIN;
- estoque e disponibilidade;
- navegação pública e carrinho anônimo com reload;
- cadastro e login CUSTOMER;
- merge do carrinho visitante;
- endereço paginado, edição e persistência;
- cotação de frete;
- checkout, número público, pedido pendente e consulta;
- requisição sem CSRF rejeitada;
- aprovação MOCK pelo endpoint autorizado de homologação, sem botão público;
- pedido confirmado e baixa/estado refletidos;
- login ADMIN, listagem, detalhe, separação, envio e entrega;
- solicitação de reembolso MOCK e consulta de estado;
- consulta de notificações;
- devolução, recebimento, inspeção e reposição idempotente;
- CUSTOMER B impedido de consultar pedido de CUSTOMER A.

`frontend/e2e/phase13-ux.spec.ts` validou viewport mobile de 375px, abertura do menu, navegação, busca, filtro, ausência de overflow horizontal e `alt` nas imagens.

`frontend/e2e/phase13-recovery.spec.ts` validou cadastro, solicitação de recuperação, captura de mensagem no Mailpit, extração do token, redefinição pelo formulário e novo login com a senha alterada.

Após a checagem complementar, a homepage foi verificada em 375, 768 e 1440 px: sem overflow horizontal e sem imagens sem `alt` nos três tamanhos. A margem negativa do hero no breakpoint mobile foi removida antes desta confirmação final.

Na primeira checagem de três tamanhos, 375 px falhou por overflow de 14 px. Após remover a margem negativa e reconstruir a imagem Docker do frontend, a repetição retornou `overflow=false` em 375/768/1440 e `missingAlt=0` nos três casos.

`frontend/e2e/smoke.spec.ts` validou a rota pública e o catálogo real parametrizado por `FRONTEND_E2E_URL`.

## Cookies, HTTPS e CSRF

O navegador observou:

- `SESSION`: `Secure=true`, `HttpOnly=true`, `SameSite=Lax`, `Path=/`.
- `XSRF-TOKEN`: `Secure=true`, `HttpOnly=false`, `SameSite=Lax`, `Path=/`.

O teste integrado confirmou mutação sem token rejeitada e mutações com token funcionando. O proxy Caddy respondeu homepage, health e OpenAPI via HTTPS. Não houve mixed content no fluxo executado.

## Falhas, correções e reexecuções

1. O teste final falhou porque `getByRole("link", {name: "Entrar"})` encontrou o link do cabeçalho e o CTA da página. O seletor foi limitado à navegação principal; cenário focado 1/1 e suíte 4/4 passaram.
2. O comando com `--project=chromium` falhou porque `playwright.config.ts` usa configuração sem projetos nomeados. O cenário foi reexecutado sem o argumento e passou.
3. Um health imediatamente após restart retornou 502 enquanto o app ainda iniciava. A rechecagem após 20 segundos retornou 200 e os dados persistidos foram confirmados.
4. Uma execução completa encontrou o rate limit de cadastro do ambiente isolado após várias reexecuções; o app foi reiniciado sem apagar dados e a repetição final passou com 5/5.

## Critérios evidenciados

Evidenciados: frontend HTTPS isolado, integração backend, cookies, CSRF, cadastro/login/logout, catálogo, imagens, carrinho anônimo/autenticado, merge, endereço, frete, checkout, idempotência coberta pelo backend/E2E, pagamento MOCK autorizado, pedidos, ADMIN, estoque, pós-venda, notificações, isolamento, responsividade, build e regressão.

Não são critérios de produção: pagamento real, SMTP real, frete comercial definitivo, storage produtivo, hospedagem, domínio e lançamento.
