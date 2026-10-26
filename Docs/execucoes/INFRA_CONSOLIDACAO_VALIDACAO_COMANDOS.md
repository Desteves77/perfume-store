# Comandos da validacao final

Os comandos abaixo foram executados nesta validacao complementar. Senhas, tokens e valores sensiveis foram omitidos ou sanitizados.

| Ordem | Diretorio | Comando | Finalidade | Resultado |
|---:|---|---|---|---|
| 1 | raiz | inspeção PowerShell de `Get-Location`, Git, Node/npm, Java, Docker, Compose, arquivos Playwright e volumes | confirmar baseline e ferramentas | falhou no primeiro agrupamento porque `java -version` escreve stderr; demais inspeções foram executadas novamente com tratamento adequado |
| 2 | raiz | `git status --short --branch`; `git rev-parse HEAD`; `git remote -v` | baseline Git | `main`, HEAD `f0687ab...`, sem mudanças e sem remotes |
| 3 | raiz | `docker version`; `docker compose version`; `docker info` | diagnóstico Docker | daemon inicialmente indisponível |
| 4 | raiz | `Start-Process -FilePath 'C:\Program Files\Docker\Docker\Docker Desktop.exe' -WindowStyle Hidden` | iniciar Docker Desktop | daemon ficou disponível |
| 5 | raiz | `docker compose up -d` | iniciar ambiente local | concluído sem remoção de volume |
| 6 | raiz | `docker compose -p perfumestore-phase13-acceptance -f compose.phase13-acceptance.yaml --env-file phase13-acceptance.env.example up -d` | iniciar homologação | concluído; serviços saudáveis |
| 7 | raiz | `./scripts/health-check.ps1 -Environment dev` | readiness local | HTTP 200 |
| 8 | raiz | `./scripts/health-check.ps1 -Environment acceptance` | readiness HTTPS | HTTP 200 |
| 9 | frontend | `$env:FRONTEND_E2E_URL='https://localhost:19680'; $env:CI='1'; node node_modules/playwright/cli.js test` | primeira tentativa Playwright | falhou por `utilsBundle` ausente em `node_modules` parcial |
| 10 | frontend | `npm ci --no-audit --no-fund --prefer-offline` | reparar dependências no Windows | ficou lento e foi interrompido; código 1 |
| 11 | frontend | `docker run --rm -v <frontend>:/src node:24-alpine sh -lc 'cd /src && npm ci --no-audit --no-fund'` | alternativa bind mount | ficou lento; container temporário foi encerrado |
| 12 | raiz | `$frontendRoot=(Resolve-Path frontend).Path; docker run --name perfumestore-e2e-deps -d -v "${frontendRoot}:/src:ro" node:24-alpine sh -lc "mkdir -p /tmp/frontend && cd /src && tar --exclude=node_modules --exclude=.next --exclude=playwright-report --exclude=test-results -cf - . | tar -xf - -C /tmp/frontend && cd /tmp/frontend && npm ci --no-audit --no-fund && sleep 3600"` | instalar dependências em filesystem temporário | concluído; npm ci adicionou 421 pacotes |
| 13 | raiz | `docker cp perfumestore-e2e-deps:/tmp/frontend/node_modules/... frontend/node_modules/...` | tentativa de reutilizar dependências no host | cópia completa encontrou falha de symlink Windows; arquivos necessários foram copiados para diagnóstico, sem alterar fontes |
| 14 | frontend | `$env:FRONTEND_E2E_URL='https://localhost:19680'; $env:CI='1'; node node_modules/playwright/cli.js test` | Playwright final | 5 passed em 43,6 s |
| 15 | raiz | `docker exec perfumestore-e2e-deps sh -lc 'cd /tmp/frontend && npm run typecheck && npm run lint && npm test && npm run build'` | qualidade frontend limpa | sucesso; Vitest 2/2 e build 22 rotas |
| 16 | backend | `$java='C:\Program Files\Zulu\zulu-17'; $env:JAVA_HOME=$java; $env:Path="$java\bin;$env:Path"; Write-Output ('JAVA_HOME=' + $env:JAVA_HOME); & "$java\bin\java.exe" -version 2>&1 | Out-String; .\mvnw.cmd --batch-mode --no-transfer-progress clean verify; exit $LASTEXITCODE` | regressão backend | 82 testes, 0 falhas, 0 erros, 0 ignorados; BUILD SUCCESS |
| 17 | raiz | `docker compose config --quiet` | validar Compose local | sucesso |
| 18 | raiz | `docker compose -f infra/compose/production.yaml --env-file infra/env/production.env.example config --quiet` | validar template prod | sucesso |
| 19 | raiz | `./scripts/health-check.ps1 -Environment dev`; `./scripts/health-check.ps1 -Environment acceptance` | revalidar health | ambos HTTP 200 |
| 20 | raiz | `curl.exe` para frontend/API local e frontend/API HTTPS | validar endpoints | quatro respostas HTTP 200 |
| 21 | raiz | `docker ps`; `docker volume ls` filtrado | preservar recursos | stacks saudáveis; volumes históricos presentes |
| 22 | raiz | `docker exec perfumestore-postgres-1 psql ...` com SQL read-only | verificar banco local | V10 e 2 usuários |
| 23 | raiz | consulta inicial ao banco de homologação com role presumido | diagnóstico | falhou com role inexistente; sem alteração |
| 24 | raiz | leitura de `POSTGRES_USER/POSTGRES_DB` sem senha e `docker exec ... psql ...` corrigido | validar banco de homologação | V10 e 40 usuários |
| 25 | raiz | `docker rm -f perfumestore-e2e-deps` | remover somente container temporário desta execução | concluído; volumes do projeto preservados |
| 26 | raiz | `git diff --check` e revisão Git final | qualidade do diff | executado antes do commit |
| 27 | raiz | `git commit -m "test: finalize consolidated infrastructure e2e validation"` | registrar validação complementar | commit criado |
| 28 | raiz | `git status --short --branch`; `git rev-parse HEAD`; `git log -1 --oneline`; `git diff --check`; `docker ps`; `docker volume ls`; `curl.exe` nos quatro endpoints | verificação pós-commit | árvore limpa, hash final conferido, serviços saudáveis, quatro respostas HTTP 200 |
| 29 | raiz | `git add Docs/execucoes/INFRA_CONSOLIDACAO_VALIDACAO_FINAL.md Docs/execucoes/INFRA_CONSOLIDACAO_VALIDACAO_COMANDOS.md`; `git diff --cached --check`; `git commit --amend --no-edit` | registrar o primeiro hash e a sequência final | amend concluído |
| 30 | raiz | `git rev-parse HEAD`; `git status --short --branch`; `git diff --check` | conferir o amend intermediário | árvore limpa e diff válido |
| 31 | raiz | `git add Docs/execucoes/INFRA_CONSOLIDACAO_VALIDACAO_FINAL.md Docs/execucoes/INFRA_CONSOLIDACAO_VALIDACAO_COMANDOS.md`; `git diff --cached --check`; `git commit --amend --no-edit` | remover referência a hash transitório dos documentos | amend final concluído |
| 32 | raiz | `git status --short --branch`; `git rev-parse HEAD`; `git log -1 --oneline --decorate`; `git diff --check`; `docker ps`; `curl.exe` nos quatro endpoints; `Select-String` para comandos proibidos | verificação final | árvore limpa, serviços saudáveis, quatro respostas HTTP 200, nenhum comando proibido executado |
| 33 | raiz | `git status --short --branch`; `git rev-parse HEAD`; `git log -1 --oneline --decorate`; `git diff --check` | conferência após o amend | árvore limpa; HEAD conferido |
| 34 | raiz | `git add Docs/execucoes/INFRA_CONSOLIDACAO_VALIDACAO_COMANDOS.md`; `git diff --cached --check`; `git commit --amend --no-edit` | incluir a conferência final no registro | commit complementar atualizado |
| 35 | raiz | `git status --short --branch`; `git rev-parse HEAD`; `git log -1 --oneline --decorate`; `git diff --check` | leitura final do hash e da árvore | árvore limpa; hash final conferido |

Quando uma linha foi composta por variáveis PowerShell ou por chamadas internas de ferramenta, os valores locais foram substituídos por `<frontend>`/`<redacted>`; essa limitação não altera o resultado nem expõe credenciais.
