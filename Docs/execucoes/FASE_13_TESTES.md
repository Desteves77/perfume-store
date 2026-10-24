# Fase 13 — testes e homologação

## Frontend

| Verificação | Resultado |
|---|---|
| `npm run typecheck` | aprovado |
| `npm run lint` | aprovado, 0 erros |
| `npm test` | 1 arquivo, 2 testes aprovados |
| `npm run build` | aprovado; 22 rotas |
| `npm run test:e2e` | 2 cenários aprovados |
| `npm start` após build final | standalone respondeu HTTP 200 |

## E2E executado

Com `NEXT_PUBLIC_API_URL=https://localhost:18680`, o Playwright validou a navegação pública e a consulta real `GET /api/v1/catalog/products?page=0&size=12&sort=name,asc` contra o backend HTTPS, além da renderização do catálogo.

## Backend e banco

Com Java 17.0.16, Maven Wrapper 3.9.16 e Testcontainers PostgreSQL 18.6:

- 82 testes executados.
- 82 aprovados.
- 0 falhas.
- 0 erros.
- 0 ignorados.
- Migrations V1–V10 aplicadas em bancos de teste.
- Classes abrangidas: AddressShipping, AdminProvisioning, ApiContract, Authentication, Cart, Catalog, Infrastructure, Inventory, Notification, Order, Payment, PostSale, ProductionProfileIsolation e SessionPersistence.

## Infraestrutura observada

- `https://localhost:18680/actuator/health/liveness`: HTTP 200, `UP`.
- `https://localhost:18680/actuator/health/readiness`: HTTP 200, `UP`.
- `https://localhost:18680/v3/api-docs`: HTTP 200.
- Docker da Fase 12 permaneceu ativo e seus volumes foram preservados.

## Falhas e reexecuções

- Instalação npm inicial interrompida; instalação verbosa e depois `npm ci` concluídas.
- Primeira suíte Vitest falhou por coleta de E2E e expectativa de rótulo; configuração e teste corrigidos, final aprovado.
- Primeiro Playwright não encontrou Chromium; Chromium foi instalado.
- Um smoke Playwright falhou por seletor ambíguo; seletor corrigido, final 2/2 aprovado.
- ESLint inicialmente encontrou artefatos `.next`; ignore corrigido, final aprovado.
- Primeira tentativa Maven na raiz falhou sem POM; execução em `backend/` finalizou com `BUILD SUCCESS`.
- `next dev --experimental-https` não conseguiu gerar certificado via mkcert; tentativa e `curl` HTTPS local falharam. Não foram versionadas chaves ou certificados.

## Escopo não afirmado

Não foram marcados como aprovados: E2E autenticado completo, E2E administrativo/pós-venda, teste de sessão/CSRF no navegador, smoke frontend sobre HTTPS próprio e liberação comercial.
