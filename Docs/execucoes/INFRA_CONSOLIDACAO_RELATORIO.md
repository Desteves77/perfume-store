# Consolidacao de ambientes e infraestrutura Docker

## Baseline

- Diretorio: `C:\Users\User\Desktop\Ecommerce PerfumeStore`.
- Branch: `main`.
- HEAD inicial: `91551ee655cbffb6c6bb4b9b4fdf1c60290945ac`.
- Arvore inicial limpa; nenhum remote configurado.
- Java do backend: Java 17; Maven Wrapper 3.9.16.
- Docker Engine 29.5.3; Docker Compose 5.1.4.
- Migrations V1 a V10 preservadas.

## Inventario

Foram inventariados os Compose raiz, catalogo, Fases 6 a 13, os exemplos de ambiente, Dockerfiles, Caddyfiles, scripts, redes, containers e volumes. Os ambientes da Fase 12 e da Fase 13 continuam preservados e identificaveis. O banco local usa o volume `perfumestore_perfumestore-postgres-data` e a porta 54329. Nenhum container historico, rede, volume, banco ou backup foi removido.

## Diagnostico PostgreSQL

O erro `28P01` da Fase 13 foi reproduzido nos logs e comparado com o volume persistente. O banco da Fase 13 possuia o database `perfumestore_phase13` e o role `phase13_user`, enquanto o container `app` stale tentava usar `perfumestore_local`/`perfumestore`. A causa foi divergencia de configuracao, nao senha perdida ou banco corrompido.

A correcao foi somente de configuracao/container: o Compose atual foi aplicado e o `app` foi recriado com `--no-deps --force-recreate`. Nao houve troca de senha, alteracao de role, recriacao de database, exclusao de volume ou alteracao de migration. O log posterior confirmou conexao TCP, Flyway V10 e aplicacao iniciada.

## Consolidacao implementada

- `compose.yaml` e a entrada local padrao: PostgreSQL, Mailpit, backend e frontend.
- Portas locais: PostgreSQL 54329, API 18081, frontend 3100, SMTP 1025 e Mailpit UI 8025, todas em loopback.
- A API usa `postgres:5432` dentro da rede Compose e recebe as mesmas credenciais do servico PostgreSQL.
- `infra/compose/production.yaml` e apenas template de producao: nao cria PostgreSQL, Mailpit, pagamento MOCK nem frete sintetico.
- `infra/env/` separa templates de ambiente; nenhum segredo real foi versionado.
- `infra/legacy/README.md` classifica configuracoes historicas sem remove-las.
- Scripts PowerShell seguros foram adicionados para iniciar/parar e verificar desenvolvimento/homologacao. Paradas usam `docker compose stop` e preservam volumes.
- A documentacao operacional foi consolidada em `Docs/infraestrutura/01-ambientes-docker.md`.

## Correcoes encontradas durante a validacao

1. A primeira inicializacao local falhou porque a porta 8080 estava ocupada. As portas publicadas foram ajustadas para 18081/3100; o servico interno continua na porta 8080.
2. Uma primeira matriz de `docker compose config` usou nomes de env inexistentes e falhou sem alterar recursos. A matriz foi corrigida para os nomes reais e todos os Compose passaram.
3. Uma consulta SQL de persistencia referenciou `app_user` sem schema/nome correto. A consulta foi corrigida para `perfumestore.app_users`; Flyway V10 e 2 usuarios foram confirmados.
4. A instalacao npm no Windows ficou lenta/incompleta. A validacao isolada em Node 24 Alpine concluiu `npm ci`, TypeScript, ESLint, Vitest (2 testes) e build Next.js (22 rotas). O download de uma imagem Playwright dedicada foi interrompido por lentidao; os E2E Playwright nao foram reexecutados nesta consolidacao.
5. Depois de reinicios, o daemon Docker reportou uma falha de execucao do healthcheck por rootfs ausente, embora a API respondesse 200. O container `app` local foi recriado sem dependencias e sem tocar no banco; o novo container ficou `healthy` e o healthcheck voltou a 200.

## Validacao final

- Todos os Compose ativos e historicos avaliados passaram em `docker compose config --quiet`, incluindo o template de producao.
- O template de producao foi verificado com servico unico `app`, sem PostgreSQL/Mailpit e sem marcador MOCK.
- Stack local: backend, frontend, Mailpit e PostgreSQL saudaveis; readiness e frontend responderam HTTP 200.
- Homologacao Fase 13: Caddy HTTPS, backend, frontend, Mailpit e PostgreSQL ativos; readiness HTTPS respondeu 200; catalogo respondeu 200.
- Restart de app/frontend local e de app/frontend/https da Fase 13 preservou os volumes. Flyway confirmou versao 10; o banco local manteve 2 usuarios.
- Backend: `clean verify` com Java 17, 82 testes, 0 falhas, 0 erros e 0 ignorados.
- Frontend isolado: typecheck, lint, Vitest 2/2 e build passaram.
- `git diff --check` passou. A verificacao de segredos nao encontrou valores reais nos arquivos operacionais; placeholders e referencias de configuracao foram mantidos.

## Preservacao e pendencias

Todos os volumes historicos foram preservados. Nao foi executado `down -v`, `docker system prune`, `docker volume prune`, reset de Git, limpeza de banco ou alteracao de migration. A limpeza de containers/volumes parados fica para decisao futura e autorizada.

Permanecem fora do escopo: credenciais externas de producao, banco/SMTP/pagamento/frete comerciais, deploy e push. A suite Playwright nao foi reexecutada nesta fase porque a imagem dedicada nao concluiu o download no ambiente; os testes E2E historicos nao foram tratados como resultado novo.

## Estado final

Infraestrutura local e de homologacao consolidada, inicializavel por `docker compose up -d`, com configuracao de producao separada e dados preservados. A validacao de infraestrutura foi concluida; a homologacao Playwright desta consolidacao permanece pendente.

Commit local criado com a mensagem `refactor: consolidate docker environments and configuration`. O hash final e a arvore sao registrados pela verificacao Git posterior e na resposta de encerramento; nao houve push, deploy ou tag.
