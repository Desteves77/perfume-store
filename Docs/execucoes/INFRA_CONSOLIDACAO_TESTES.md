# Testes da consolidacao de infraestrutura

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
