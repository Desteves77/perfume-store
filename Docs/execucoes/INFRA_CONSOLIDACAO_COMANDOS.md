# Comandos da consolidacao de infraestrutura

Os comandos abaixo sao os comandos shell registrados nesta execucao. Segredos foram omitidos como `[REDACTED]`. Algumas consultas foram executadas por composicao interna; quando o executor nao forneceu um codigo individual, isso esta indicado.

| # | Diretorio | Comando sanitizado | Finalidade | Exit | Resultado |
|---:|---|---|---|---:|---|
| 1 | raiz | `git status --short --branch; git rev-parse HEAD; git remote -v` | Baseline Git | nao exposto individualmente | `main`, HEAD `91551ee655cbffb6c6bb4b9b4fdf1c60290945ac`, sem remotes |
| 2 | raiz | `docker ps -a; docker network ls; docker volume ls` | Inventario Docker | nao exposto individualmente | Containers, redes e volumes historicos identificados |
| 3 | raiz | `docker logs --tail 100 perfumestore-phase13-acceptance-app-1; docker logs --tail 100 perfumestore-phase13-acceptance-postgres-1` | Diagnostico PostgreSQL | nao exposto individualmente | Log indicou role esperada ausente |
| 4 | raiz | `docker compose -p perfumestore-phase13-acceptance -f compose.phase13-acceptance.yaml --env-file phase13-acceptance.env.example config --format json` | Inspecao de Compose | nao exposto individualmente | Configuracao real usa role/database da Fase 13 |
| 5 | raiz | `docker inspect perfumestore-phase13-acceptance-app-1 perfumestore-phase13-acceptance-postgres-1` com credenciais redigidas | Comparacao de containers | nao exposto individualmente | App stale divergente do volume |
| 6 | raiz | `docker exec ... pg_isready ...; docker exec ... psql ... select current_user,current_database()` | Consulta read-only do banco | nao exposto individualmente | Roles/databases confirmados sem escrita |
| 7 | raiz | `docker compose -p perfumestore-phase13-acceptance -f compose.phase13-acceptance.yaml --env-file phase13-acceptance.env.example up -d --no-deps --force-recreate app` | Corrigir app stale | 0 | App conectou sem alterar banco/volume |
| 8 | raiz | `docker compose ... ps app postgres; docker inspect ...; docker logs ...` | Validar correcao | 0 | Hikari, Flyway V10 e startup confirmados |
| 9 | raiz | `docker compose config --quiet` | Validar Compose raiz | 0 | Configuracao valida |
| 10 | raiz | `docker compose -f infra/compose/production.yaml --env-file infra/env/production.env.example config --quiet` | Validar template prod | 0 | Configuracao valida |
| 11 | raiz | `docker compose up -d --build` | Primeira inicializacao local | 1 | Falhou somente por bind de 8080 ocupado |
| 12 | raiz | `Get-NetTCPConnection ...; netsh interface ipv4 show excludedportrange protocol=tcp` | Investigar portas | 0 | 8080 ocupado; 18081/3100 livres |
| 13 | raiz | `docker compose up -d` | Inicializacao apos ajuste de portas | 0 | Stack local iniciada |
| 14 | raiz | `curl.exe ...; docker exec ... psql ...; docker logs ...` | Health/catalogo/Flyway | 0 | HTTP 200; Flyway V10; dados observados preservados |
| 15 | raiz | `docker compose restart app frontend; curl.exe ...; docker exec ... psql ...` | Persistencia local | 0 | Restart concluido e servicos responderam |
| 16 | raiz | `.\scripts\dev-start.ps1` | Exercitar script local | 0 | Configurou, construiu e iniciou o ambiente |
| 17 | raiz | `.\scripts\health-check.ps1 -Environment dev; .\scripts\health-check.ps1 -Environment acceptance` | Health local/HTTPS | 0 | Ambos HTTP 200 |
| 18 | raiz | `docker compose -p perfumestore-phase13-acceptance ... up -d --build --force-recreate; ... ps` | Reiniciar homologacao | 0 | App/frontend/https/postgres/mailpit ativos |
| 19 | raiz | `docker compose ... config --quiet` com env inexistente | Primeira matriz Compose | 1 | Falhou por nomes de env incorretos; nenhum recurso alterado |
| 20 | raiz | matriz `docker compose -f ... config --quiet` para Compose raiz, historicos e prod | Validar configuracoes reais | 0 | Todos os nove arquivos passaram |
| 21 | raiz | `docker compose -f infra/compose/production.yaml --env-file ... config --format json` e verificacao de servicos/marcadores | Revisar isolamento prod | 0 | Apenas `app`; sem PostgreSQL/Mailpit/MOCK |
| 22 | raiz | varredura inicial de segredos em arquivos Git | Procurar segredos | 1 | Falso positivo por texto de comando/documentacao |
| 23 | raiz | varredura repetida excluindo registros de comandos/testes/relatorios | Revalidar segredos | 0 | Nenhum segredo real encontrado nos arquivos operacionais |
| 24 | `backend` | `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd --batch-mode --no-transfer-progress clean verify` | Regressao backend | 0 | 82 testes; 0 falhas; 0 erros; 0 ignorados; BUILD SUCCESS |
| 25 | `frontend` | `npm ci --no-audit --no-fund` | Instalar dependencias Windows | 1 | Interrompido por lentidao/reificacao |
| 26 | `frontend` | `npm run typecheck` | TypeScript no host | 1 | `tsc` ausente devido instalacao incompleta |
| 27 | `frontend` | `npm ci --offline ...`, `npm install --offline ...`, `npm ci --loglevel=verbose`, `npm install --no-package-lock ...` | Repetir instalacao host | 1 em cada tentativa | Interrompidos por lentidao; sem alteracao de fonte |
| 28 | raiz | `docker pull node:24-alpine` | Isolar toolchain frontend | 0 | Imagem obtida |
| 29 | raiz | `docker run --rm ... node:24-alpine ... npm ci ...` | Qualidade frontend isolada | 1/interrompido | Primeira tentativa incluiu arvore de dependencias parcial |
| 30 | raiz | `docker run --rm --pull=never -v "${sourceRoot}:/src:ro" node:24-alpine sh -lc "... tar --exclude=node_modules ... npm ci ... typecheck ... lint ... test ... build"` | Qualidade frontend limpa | 0 | npm ci, typecheck, lint, Vitest 2/2 e build Next.js 22 rotas |
| 31 | raiz | `docker pull mcr.microsoft.com/playwright:v1.55.0-noble` | Preparar navegador E2E isolado | 1/interrompido | Download lento; imagem nao concluiu |
| 32 | raiz | `.\scripts\health-check.ps1 -Environment dev; .\scripts\health-check.ps1 -Environment acceptance` | Revalidar health | 0 | Local e HTTPS 200 |
| 33 | raiz | `docker compose restart app frontend; docker compose -p perfumestore-phase13-acceptance ... restart app frontend https; Start-Sleep 35; ... health-check ...` | Persistencia apos restart integrado | 1 | Servicos responderam 200; consulta SQL final referenciou tabela incorreta |
| 34 | raiz | `docker exec perfumestore-postgres-1 psql ... select ... perfumestore.flyway_schema_history ... perfumestore.app_users; docker compose ps` | Corrigir consulta e validar estado | 0 | Flyway V10; 2 usuarios; containers ativos |
| 35 | raiz | `docker volume ls --format '{{.Name}}'; docker ps --format ...` | Confirmar preservacao | 0 | Volumes e containers historicos presentes |
| 36 | raiz | `git diff --check` | Revisao de whitespace | 0 | Apenas avisos CRLF/LF; nenhuma falha |
| 37 | raiz | `docker rm -f festive_noether` | Remover somente container temporario criado para teste | 0 | Container temporario removido; containers/volumes do projeto preservados |
| 38 | raiz | `docker compose up -d --no-deps --force-recreate app; Start-Sleep 45; docker compose ps; .\scripts\health-check.ps1 -Environment dev` | Corrigir healthcheck Docker apos rootfs transitorio | 0 | App local terminou `healthy`; readiness HTTP 200 |
| 39 | raiz | `git commit -m "refactor: consolidate docker environments and configuration"` | Criar commit local | 0 | Commit criado; sem push/deploy |
| 40 | raiz | `git rev-parse HEAD; git status --short --branch; git remote -v` | Confirmar encerramento Git | 0 | Hash e arvore final registrados na resposta; sem remote |
| 41 | raiz | `git add Docs/execucoes/INFRA_CONSOLIDACAO_*.md; git commit --amend --no-edit` | Incluir o fechamento dos registros no mesmo commit local | 0 | Commit final mantido como um unico commit local |
| 42 | raiz | `git add Docs/execucoes/INFRA_CONSOLIDACAO_COMANDOS.md; git commit --amend --no-edit` | Corrigir a ordem final do registro de comandos | 0 | Commit local final atualizado; sem push/deploy |

Operacoes de edicao foram feitas com `apply_patch`; essa ferramenta nao fornece uma linha shell reproduzivel. Nenhum push, deploy, tag ou comando destrutivo foi executado.
