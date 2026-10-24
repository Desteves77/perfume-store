# Fase 13 — continuação: relatório de homologação integrada

## Estado inicial

- Diretório: `C:\Users\User\Desktop\Ecommerce PerfumeStore`.
- Branch: `main`.
- HEAD inicial: `14ce6fcb66b8a3a778798fd682c21a4371422720`.
- Árvore inicial limpa e sem remotes configurados.
- Frontend existente em `frontend/`, Next.js 15.5.28, React 19.1, TypeScript, Tailwind e Playwright.
- Backend preservado, Java 17 pelo Maven Wrapper, Spring Boot 4.1.1, Flyway V1–V10.
- PostgreSQL de desenvolvimento não foi utilizado nem alterado.
- O ambiente anterior da Fase 12 foi preservado. A homologação desta continuação utilizou o projeto isolado `perfumestore-phase13-acceptance`.

## Plano e matriz executada

| Jornada | Módulos | Evidência | Resultado |
|---|---|---|---|
| Público | Next, catálogo, carrinho | Playwright em HTTPS | Aprovada |
| Cadastro/login/logout | auth, sessão, CSRF, cliente | Playwright e cookies reais | Aprovada |
| Recuperação de senha | auth, notification, Mailpit | Cadastro, solicitação, token do e-mail, redefinição e novo login | Aprovada |
| Merge | cart, auth | Carrinho anônimo antes do login e recuperação após login | Aprovada |
| Endereço/frete | address, shipping, checkout | Cadastro, edição, cotação e persistência | Aprovada |
| Checkout | cart, order, inventory | Pedido real com chave idempotente | Aprovada |
| Pagamento | payment MOCK autorizado | Evento aprovado via fluxo de homologação | Aprovada |
| Administração | catalog, inventory, order | Login ADMIN e ações REST reais | Aprovada |
| Pós-venda | refund, return, inventory | Reembolso MOCK, recebimento, inspeção e reposição | Aprovada |
| Notificações | notification, Mailpit | Consulta administrativa e eventos | Aprovada |
| Isolamento | security, order, payment | CUSTOMER B tentando acessar pedido de A | Aprovada |
| Responsividade | Next/CSS | Viewport 375px, menu e overflow | Aprovada |

## Ambiente isolado

Foi criado `compose.phase13-acceptance.yaml` com PostgreSQL 18.6, aplicação Spring Boot, frontend Next standalone, Caddy 2.10.2 e Mailpit. Portas locais sintéticas:

- HTTPS: `https://localhost:19680`.
- HTTP de redirecionamento: `http://localhost:19681`.
- PostgreSQL: `127.0.0.1:56610`.
- SMTP Mailpit: `127.0.0.1:11435`.
- Mailpit UI: `http://localhost:18635`.

O Caddy encaminha `/` para o frontend e `/api/*`, `/actuator/*` e documentação para o backend. O certificado é interno/local e não representa certificado público. Os volumes são exclusivos desta homologação e não foram removidos.

## Correções realizadas

1. `GET /api/v1/me/addresses` retorna `PageResponse`; as telas de endereços e checkout consumiam anteriormente uma lista direta. O frontend passou a usar `Page<Address>.items` com paginação explícita.
2. O painel administrativo criava produto, mas não oferecia publicação. Foi adicionada a ação real `PUT /api/v1/admin/products/{id}/status` com versão do recurso.
3. A tela de detalhe administrativo não expunha as operações de recebimento, inspeção e reposição. Foram adicionados controles para os endpoints existentes, com `orderItemId` vindo da API e sem SQL.
4. O teste integrado tinha seletor ambíguo para `Entrar` após logout. O seletor foi restringido à navegação principal.
5. O smoke público usava origem padrão HTTP/8080 mesmo quando executado sob Caddy HTTPS. Foi parametrizado por `FRONTEND_E2E_URL`.
6. A verificação complementar em 375 px encontrou overflow horizontal de 14 px causado pela margem negativa do hero. A margem foi removida no breakpoint mobile e a verificação visual foi repetida.
7. O primeiro teste de recuperação perdeu a mensagem transitória por redirecionamento programado; foi ajustado para aguardar a URL. A seleção inicial do e-mail procurava `senha`, mas o assunto real era `Recuperacao de acesso`; o teste passou a aceitar o contrato real.
8. Uma rodada completa encontrou rate limit de cadastro do ambiente sintético após várias reexecuções. A aplicação foi reiniciada sem tocar no banco/volume e a suíte passou com 5/5.

Nenhuma migration, domínio comercial ou código funcional do backend foi alterado.

## Segurança e contratos

- O navegador executou mutações com CSRF; uma mutação sem token recebeu rejeição e a mesma jornada com token funcionou.
- Cookies observados por Playwright: `SESSION` com `Secure`, `HttpOnly`, `SameSite=Lax`, caminho `/`; `XSRF-TOKEN` com `Secure`, `SameSite=Lax`, caminho `/`.
- Sessão não foi armazenada em `localStorage`.
- O isolamento de cliente foi exercitado com CUSTOMER B tentando abrir pedido de CUSTOMER A.
- O pagamento MOCK foi acionado somente pelo mecanismo de homologação autorizado, não por botão público de aprovação.
- OpenAPI via Caddy respondeu 200 e continha as rotas de autenticação, carrinho, checkout, administração, notificações e devolução usadas no E2E.

## Resultados

- Frontend: typecheck aprovado.
- ESLint: aprovado.
- Vitest: 2 testes aprovados.
- Build Next.js: aprovado, 22 rotas.
- Playwright final: 5 testes aprovados.
- Backend `clean verify`: 82 testes, 0 falhas, 0 erros, 0 ignorados.
- JAR backend: `backend/target/perfumestore-backend-0.0.1-SNAPSHOT.jar` gerado.
- Flyway: V1–V10 aplicadas nos bancos reais de teste e V10 preservada no ambiente isolado.
- Restart: app, frontend e Caddy reiniciados sem remover o PostgreSQL; health voltou a 200 após o startup e os 13 produtos sintéticos permaneceram disponíveis.

## Falhas e reexecuções

- Uma execução final do Playwright falhou por ambiguidade do seletor `Entrar`; corrigida e reexecutada com 1/1 no cenário integrado e 4/4 na suíte completa.
- Uma tentativa focada usou `--project=chromium`, mas a configuração não declara projetos nomeados; o comando foi rejeitado antes dos testes. Reexecutado sem esse argumento e aprovado.
- Logo após restart, health retornou 502 enquanto a aplicação estava `health: starting`; após aguardar a prontidão, health retornou 200. Nenhum dado foi perdido.
- A regressão Maven exibiu avisos de conexões Testcontainers encerradas durante a limpeza de contextos, sem falhas de teste; o resultado final foi 82/82 e `BUILD SUCCESS`.

## Limitações

- Pagamento continua MOCK e restrito a homologação.
- SMTP real, frete comercial definitivo, storage produtivo, hospedagem, domínio e lançamento não estão configurados.
- O certificado HTTPS local é apenas de homologação.
- SMTP não oferece garantia universal de entrega exatamente uma vez; a evidência é de processamento no ambiente isolado.
- A primeira parte da Fase 13 possui registros históricos que reportavam cobertura apenas pública; este documento registra a continuação e não sobrescreve aqueles registros.

## Estado final

O MVP frontend foi integrado e homologado tecnicamente em ambiente isolado com navegador real, backend real, PostgreSQL real de homologação, HTTPS local e dados sintéticos. Não houve push, deploy, tag ou início da Fase 14.

Commit da continuação: commit local criado com a mensagem `test: complete frontend integration and end-to-end acceptance`; o hash completo foi confirmado por `git rev-parse HEAD` no encerramento e consta no handoff final.
