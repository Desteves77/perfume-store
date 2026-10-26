# Testes da consolidacao de infraestrutura

## Validacao complementar executada em 26/10/2026

| Area | Comando/ambiente | Resultado |
|---|---|---|
| Docker daemon | Docker Desktop 29.5.3 | Iniciado sem alteracao de volumes |
| Compose local | `docker compose config --quiet` e `docker compose up -d` | Aprovado; servicos saudaveis |
| Compose producao | `docker compose -f infra/compose/production.yaml --env-file infra/env/production.env.example config --quiet` | Aprovado; template sem PostgreSQL/Mailpit/MOCK |
| HTTPS | `https://localhost:19680` | Readiness, frontend e catalogo: HTTP 200 |
| Playwright | `FRONTEND_E2E_URL=https://localhost:19680; CI=1; node node_modules/playwright/cli.js test` | 5/5 aprovados, 43,6 s |
| Frontend | Node 24 Alpine, `npm run typecheck`, `npm run lint`, `npm test`, `npm run build` | TypeScript/ESLint aprovados; Vitest 2/2; 22 rotas |
| Backend | Java 17, `mvnw.cmd --batch-mode --no-transfer-progress clean verify` | 82 testes; 0 falhas; 0 erros; 0 ignorados; BUILD SUCCESS |
| Flyway/persistencia | consultas read-only nos bancos local e de homologacao | V10; 2 e 40 usuarios, respectivamente |
| Seguranca | fluxos Playwright e `ProductionProfileIsolationIntegrationTest` | CSRF/isolamento aprovados; perfil prod sem MOCK |

### Falhas e reexecucoes

- O daemon Docker estava parado; foi iniciado e os dois ambientes foram levantados com `up -d`, sem `down -v`.
- O primeiro CLI Playwright falhou por `utilsBundle` ausente em `node_modules` parcial. `npm ci` no Windows permaneceu lento; uma instalacao limpa Node 24 em container temporario resolveu o problema. O Chromium 1187 do cache local foi reutilizado.
- A primeira copia completa de `node_modules` para Windows falhou em symlinks por privilegio do sistema. Isso nao afetou o projeto versionado; a execucao final ocorreu dentro do container isolado e passou.
- A primeira consulta SQL de homologacao usou um role presumido inexistente. O role real foi lido do arquivo de ambiente sem imprimir senha, e a consulta read-only corrigida confirmou V10 e 40 usuarios.

### Cobertura

Os 5 testes Playwright foram executados contra o backend real HTTPS e nao contra mocks de API. O backend utilizou PostgreSQL real via Testcontainers no `clean verify`. Nenhum banco ou volume historico foi limpo.

## Resultados aprovados

| Area | Evidencia | Resultado |
|---|---|---|
| Docker Compose | Matriz com raiz, historicos Fases 4.1/5 a 13 e `infra/compose/production.yaml` | Todos passaram em `config --quiet` |
| PostgreSQL | Containers, `pg_isready`, consultas read-only e logs | Desenvolvimento e Fase 13 autenticaram; dados preservados |
| Diagnostico 28P01 | Comparacao entre role/database do volume e ambiente do app | Causa comprovada como divergencia stale de configuracao |
| Correcao | Recriacao somente do app com `--no-deps --force-recreate` | App conectado; Flyway V10 validado |
| Local | `docker compose up -d`, health e catalogo | Backend/frontend/Mailpit/PostgreSQL saudaveis; HTTP 200 |
| Homologacao | Fase 13 com Caddy | HTTPS 19680 e catalogo/readiness HTTP 200 |
| Persistencia | Restart de app/frontend e de Fase 13 sem remover volumes | Flyway V10 e 2 usuarios locais preservados |
| Backend | Maven Wrapper com Java 17 e `clean verify` | 82 testes; 0 falhas; 0 erros; 0 ignorados |
| Frontend | Node 24 Alpine, `npm ci`, `typecheck`, `lint`, `vitest`, `build` | TypeScript/ESLint aprovados; Vitest 2/2; Next.js 22 rotas |
| Producao | Compose renderizado como JSON e verificacao de servicos | Somente `app`; sem PostgreSQL, Mailpit ou MOCK |
| Segredos | Varredura operacional sem valores reais | Aprovada; placeholders documentados |
| Diff | `git diff --check` | Aprovado |

## Falhas e reexecucoes

- O primeiro `docker compose up -d --build` falhou porque a porta 8080 estava ocupada. A configuracao foi ajustada para publicar a API em 18081 e o frontend em 3100; a segunda inicializacao passou.
- A primeira matriz de Compose apontou para nomes de env inexistentes. A matriz foi corrigida com os nomes reais e passou integralmente.
- Uma consulta de persistencia usou `app_user`; a tabela real e `perfumestore.app_users`. A consulta corrigida confirmou V10 e 2 usuarios.
- O `npm ci` no Windows ficou preso em reificacao; typecheck host falhou por `tsc` ausente. O conjunto foi reexecutado em container Node 24 limpo e passou.
- O download da imagem Playwright `mcr.microsoft.com/playwright:v1.55.0-noble` foi interrompido por lentidao. Os cinco testes Playwright nao foram executados nesta consolidacao e permanecem pendencia explicita; o baseline historico nao foi contado como resultado novo.
- Apos reinicio, o healthcheck do app local apresentou erro de rootfs do daemon Docker, apesar de readiness 200. A recriacao pontual do container `app` corrigiu o estado; o container terminou `healthy` e o healthcheck voltou a 200.

## Cobertura nao aplicavel ou pendente

- Nenhuma migration foi criada ou alterada.
- Nao houve transacao comercial nova nesta fase; o checkout MOCK e as operacoes administrativas foram preservados nos ambientes historicos, nao reexecutados como nova suite funcional.
- O teste E2E Playwright deve ser reexecutado quando a infraestrutura de navegador/dependencias estiver disponivel.
