# Fase 13 — continuação: comandos executados

Os comandos abaixo são os comandos shell reproduzíveis que ficaram disponíveis nesta execução. Segredos sintéticos de homologação foram mantidos apenas como valores locais de exemplo; não há credenciais reais.

| Ordem | Diretório | Comando sanitizado | Finalidade | Código | Resultado |
|---:|---|---|---|---:|---|
| 1 | raiz | `git status --short --branch; git diff --name-only; git remote -v` | Confirmar branch, árvore e remotes | 0 | `main`, sem remotes; árvore inicial limpa antes das alterações |
| 2 | raiz | `docker compose -p perfumestore-phase13-acceptance -f compose.phase13-acceptance.yaml --env-file phase13-acceptance.env.example config --quiet` | Validar Compose isolado | 0 | Configuração válida |
| 3 | raiz | `docker compose -p perfumestore-phase13-acceptance -f compose.phase13-acceptance.yaml --env-file phase13-acceptance.env.example up -d --build` | Construir e iniciar PostgreSQL, app, frontend, Caddy e Mailpit | 0 | Imagens construídas e stack iniciada |
| 4 | raiz | `docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"; curl.exe -k -I https://localhost:19680/; curl.exe -k -sS https://localhost:19680/api/v1/catalog/products?page=0\&size=1` | Primeira verificação HTTP/HTTPS | não exposto | A tentativa com `&` sem aspas foi interpretada pelo PowerShell e falhou; reexecutada com URL entre aspas |
| 5 | raiz | `docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"; curl.exe -k -I "https://localhost:19680/"; curl.exe -k -sS "https://localhost:19680/api/v1/catalog/products?page=0&size=1"` | Repetir homepage e catálogo com sintaxe correta | 0 | Homepage 200; catálogo respondeu |
| 6 | raiz | `docker compose -p perfumestore-phase13-acceptance -f compose.phase13-acceptance.yaml --env-file phase13-acceptance.env.example ps` | Verificar containers e health | 0 | PostgreSQL, app, frontend, Caddy e Mailpit ativos |
| 7 | frontend | `npm run typecheck` | Verificar TypeScript | 0 | Aprovado |
| 8 | frontend | `npm run lint` | Verificar ESLint | 0 | Aprovado |
| 9 | frontend | `npm test` | Executar Vitest | 0 | 2 testes aprovados |
| 10 | frontend | `npm run build` | Gerar build Next.js | 0 | Build aprovado; 22 rotas |
| 11 | frontend | `$env:CI='1'; $env:FRONTEND_E2E_URL='https://localhost:19680'; npm run test:e2e` | Executar E2E HTTPS após correções iniciais | 0 | 4 testes aprovados numa execução anterior |
| 12 | backend | `java -version; .\mvnw.cmd -version; .\mvnw.cmd clean verify` | Executar regressão e build final com Maven Wrapper | 0 | Java Wrapper 17.0.16; 82 testes; JAR gerado; BUILD SUCCESS |
| 13 | raiz | `curl.exe -k -sS -o NUL -w "homepage=%{http_code}\n" "https://localhost:19680/"; curl.exe -k -sS -o NUL -w "health=%{http_code}\n" "https://localhost:19680/actuator/health"; curl.exe -k -sS -o NUL -w "openapi=%{http_code}\n" "https://localhost:19680/v3/api-docs"; curl.exe -k -sS "https://localhost:19680/api/v1/catalog/products?page=0&size=1&sort=name,asc"` | Verificar homepage, health, OpenAPI e persistência do catálogo | 0 | 200, 200, 200; dados sintéticos presentes |
| 14 | backend | `Get-ChildItem -Path target -Filter "perfumestore-backend-*.jar" ...` com soma dos XMLs Surefire | Confirmar JAR e contagem de testes | 0 | JAR presente; 14 relatórios; 82/82 |
| 15 | frontend | `node -e "... chromium ... fetch('/api/v1/security/csrf') ..."` | Inspecionar atributos do cookie CSRF no navegador | 0 | `XSRF-TOKEN`: Secure, SameSite=Lax, path `/` |
| 16 | frontend | `node -e "... chromium ... login ... cookies() ..."` | Inspecionar sessão após login no navegador | 0 | `SESSION`: Secure, HttpOnly, SameSite=Lax; XSRF também seguro |
| 17 | raiz | `docker compose -p perfumestore-phase13-acceptance -f compose.phase13-acceptance.yaml --env-file phase13-acceptance.env.example restart app frontend https; Start-Sleep -Seconds 12; ...` | Reiniciar aplicação, frontend e Caddy preservando volumes | 0 | Restart concluído; primeiro health ainda 502 durante startup |
| 18 | raiz | `Start-Sleep -Seconds 20; docker compose ... ps; curl.exe -k ... health; curl.exe -k ... catalog` | Revalidar após prontidão | 0 | Todos saudáveis; health 200; 13 produtos preservados |
| 19 | frontend | `npm run typecheck; ... npm run lint; ... npm test; ... npm run build; ... npm run test:e2e` | Rodada final combinada antes do último ajuste do teste | 1 | 3 testes passaram e 1 falhou por seletor ambíguo `Entrar` |
| 20 | frontend | `npx playwright test e2e/phase13-integration.spec.ts --project=chromium` | Tentativa de execução focada | 1 | Configuração não possui projeto nomeado `chromium`; nenhum teste iniciou |
| 21 | frontend | `$env:CI='1'; $env:FRONTEND_E2E_URL='https://localhost:19680'; npx playwright test e2e/phase13-integration.spec.ts` | Reexecutar cenário focado sem projeto inexistente | 0 | 1 teste aprovado |
| 22 | frontend | `$env:CI='1'; $env:FRONTEND_E2E_URL='https://localhost:19680'; npm run test:e2e` | Reexecutar toda a suíte após correção | 0 | 4 testes aprovados |
| 23 | raiz | `$doc = (curl.exe -k -sS "https://localhost:19680/v3/api-docs" | ConvertFrom-Json); ...` | Conferir rotas OpenAPI usadas pelo frontend | 0 | Rotas de auth, cart, checkout, admin, notificações e restock presentes |
| 24 | frontend | `npm run typecheck; ... npm run lint; ... npm test; ... npm run build; ... npm run test:e2e` | Rodada final após a correção do teste | 0 | Typecheck, lint, Vitest 2/2, build 22 rotas e Playwright 4/4 aprovados |
| 25 | frontend | `node -e "... viewport [375,768,1440] ..."` | Verificar overflow e `alt` nos três tamanhos | 1 | A primeira checagem encontrou overflow em 375 px; causa identificada no hero mobile |
| 26 | frontend | `node -e "... elementos fora da viewport ..."` | Identificar elemento que excedia a largura | 0 | Hero com `left=-14` e `right=389` |
| 27 | frontend | `node -e "... métricas document ..."` | Reproduzir métricas de viewport | 0 | Confirmada largura de documento 389 para viewport 375 |
| 28 | frontend | `node -e "... viewport [375,768,1440] ..."` | Rechecagem preliminar após edição local | 1 | Container ainda usava imagem anterior; correção não estava publicada no serviço |
| 29 | raiz | `docker compose -p perfumestore-phase13-acceptance -f compose.phase13-acceptance.yaml --env-file phase13-acceptance.env.example build frontend; ... up -d frontend; ... ps` | Reconstruir somente frontend e recriar serviço | 0 | Build Next no container aprovado; frontend saudável |
| 30 | frontend | `node -e "... viewport [375,768,1440] ..."` | Validar artefato reconstruído | 0 | 375/768/1440 sem overflow e sem imagem sem `alt` |
| 31 | frontend | `npm run typecheck; ... npm run lint; ... npm test; ... npm run build; ... npm run test:e2e` | Rodada final após correção visual | 0 | Typecheck, lint, Vitest 2/2, build 22 rotas e Playwright 4/4 aprovados |
| 32 | raiz | `git add frontend/src/app/globals.css Docs/execucoes/FASE_13_HOMOLOGACAO_COMANDOS.md Docs/execucoes/FASE_13_HOMOLOGACAO_TESTES.md Docs/execucoes/FASE_13_HOMOLOGACAO_RELATORIO.md Docs/frontend/01-design-system.md; git diff --cached --check; git commit --amend --no-edit; git rev-parse HEAD; git status --short --branch; git diff --check` | Incorporar a correção visual e registros no commit local | 0 | Commit atualizado; árvore limpa; diff check aprovado |
| 33 | raiz | `git rev-parse --abbrev-ref HEAD; git rev-parse HEAD; git status --short --branch; git log -1 --oneline --decorate; git show --stat --oneline --summary HEAD; git diff HEAD^ HEAD --check` | Revisar commit e diff integral final | 0 | HEAD e estatísticas conferidos; diff check aprovado |
| 34 | raiz | `docker compose -p perfumestore-phase13-acceptance -f compose.phase13-acceptance.yaml --env-file phase13-acceptance.env.example ps; curl.exe -k ... health; curl.exe -k ... v3/api-docs` | Conferir containers, HTTPS, health e OpenAPI no encerramento | 0 | Todos os serviços ativos; 200/200/200 |
| 35 | raiz | `rg -n --hidden ... --glob '!Docs/execucoes/FASE_13_HOMOLOGACAO_COMANDOS.md' -e 'PRIVATE KEY' -e 'AKIA...' -e 'sk_live_' -e 'ghp_...' .; if ($LASTEXITCODE -eq 1) { 'secret-pattern-scan=no-matches-outside-command-log' }` | Verificar padrões de segredos, excluindo o próprio log que documenta os padrões | 0 | Nenhum padrão encontrado fora do log de comandos |
| 36 | raiz | `Get-Item Docs/execucoes/FASE_13_HOMOLOGACAO_*.md; docker volume ls ...` | Confirmar registros e volumes preservados | 0 | Três registros presentes; quatro volumes isolados presentes |
| 37 | frontend | `$env:CI='1'; $env:FRONTEND_E2E_URL='https://localhost:19680'; npx playwright test e2e/phase13-recovery.spec.ts` | Executar recuperação de senha no navegador | 1 | Primeira versão falhou na mensagem transitória do cadastro; teste corrigido |
| 38 | frontend | `$env:CI='1'; $env:FRONTEND_E2E_URL='https://localhost:19680'; npx playwright test e2e/phase13-recovery.spec.ts` | Reexecutar recuperação após aguardar URL | 1 | Captura do Mailpit revelou assunto real `Recuperacao de acesso`; teste ajustado |
| 39 | frontend | `$env:CI='1'; $env:FRONTEND_E2E_URL='https://localhost:19680'; npx playwright test e2e/phase13-recovery.spec.ts` | Reexecutar fluxo corrigido | 0 | 1 teste aprovado |
| 40 | frontend | `$env:CI='1'; $env:FRONTEND_E2E_URL='https://localhost:19680'; npm run test:e2e` | Rodar suíte completa incluindo recuperação | 1 | 3/5 passaram; cadastro foi limitado por rate limit do ambiente |
| 41 | raiz | `docker compose -p perfumestore-phase13-acceptance -f compose.phase13-acceptance.yaml --env-file phase13-acceptance.env.example restart app; Start-Sleep -Seconds 25; ... ps; curl.exe ... health` | Reiniciar somente app para limpar limitador em memória | 0 | Health 200; banco e volumes preservados |
| 42 | frontend | `$env:CI='1'; $env:FRONTEND_E2E_URL='https://localhost:19680'; npm run test:e2e` | Reexecutar suíte completa após restart | 0 | 5 testes aprovados |
| 43 | frontend | `npm run typecheck; ... npm run lint; ... npm test; ... npm run build` | Revalidar qualidade após inclusão do teste de recuperação | 0 | Typecheck, lint, Vitest 2/2 e build 22 rotas aprovados |

## Limitação de rastreabilidade

As edições feitas pela ferramenta de patch do executor não possuem uma linha shell reproduzível; foram registradas como alterações de arquivos, não como comandos inventados. Alguns comandos da criação inicial do ambiente ocorreram antes da compactação do contexto e a linha completa não ficou disponível nesta execução; por isso são identificados aqui como limitação, sem atribuir códigos ou argumentos não observados.
