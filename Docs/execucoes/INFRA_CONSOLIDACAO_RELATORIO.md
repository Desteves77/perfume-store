# Consolidacao de ambientes e infraestrutura Docker

## Validacao complementar — homologacao E2E

### Baseline e diagnostico

- HEAD de entrada: `f0687abbb83a137751331369d8d44c7960eec1e6`, branch `main`, arvore limpa e sem remotes.
- Docker daemon inicialmente indisponivel; Docker Desktop foi iniciado sem modificar volumes.
- A configuracao Playwright usa `FRONTEND_E2E_URL`; o cache local continha Chromium 1187.
- A primeira tentativa direta falhou porque a instalacao host de `node_modules` estava incompleta e o CLI nao encontrava `utilsBundle`. `npm ci` no Windows permaneceu lento. A instalacao limpa em container Node 24 foi concluida e os testes passaram.
- Nao houve alteracao de frontend, backend, migrations ou Compose nesta validacao.

### Ambiente e evidencias

- Compose local: frontend `3100`, API `18081`, PostgreSQL `54329`, Mailpit `1025/8025`; todos saudaveis.
- Homologacao: Caddy HTTPS `19680`, redirect HTTP `19681`, PostgreSQL `56610`, Mailpit `11435/18635`; app, frontend, PostgreSQL e Mailpit saudaveis.
- Frontend, API de catalogo e readiness local/HTTPS responderam `200`.
- Banco local: migration mais recente `10`, 2 usuarios.
- Banco de homologacao: migration mais recente `10`, 40 usuarios sinteticos.

### Homologacao Playwright

Com `FRONTEND_E2E_URL=https://localhost:19680` e `CI=1`, a suite existente executou 5 testes em 2 workers: `5 passed` em 43,6 s. A cobertura efetiva incluiu loja publica, carrinho anonimo, cadastro/login, merge, endereco, frete, checkout, pagamento MOCK autorizado, CSRF, historico, ADMIN, catalogo/estoque, separacao/envio/entrega, reembolso, devolucao/inspecao/reposicao, notificacoes, recuperacao de senha, responsividade e isolamento entre clientes.

### Regressao final

- Frontend em container Node 24 limpo: typecheck aprovado, ESLint aprovado, Vitest 2/2 aprovado e build Next.js aprovado com 22 rotas.
- Backend com Java 17.0.16 e Maven Wrapper: `clean verify`, 82 testes, 0 falhas, 0 erros, 0 ignorados, `BUILD SUCCESS`.
- Compose local e template de producao passaram `config --quiet`; readiness e endpoints principais permaneceram 200.

### Preservacao e estado final

O container temporario `perfumestore-e2e-deps` foi removido ao final. Nenhum container historico, banco, backup, imagem persistida ou volume foi removido. O banco local e o banco de homologacao continuaram disponiveis. A validacao complementar encerra formalmente a consolidacao de infraestrutura; nao houve push, deploy ou liberacao comercial.

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
