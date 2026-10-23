# Fase 8 — Registro de comandos

Os comandos abaixo serão registrados em ordem cronológica durante a execução. Segredos, quando eventualmente presentes, serão substituídos por `[REDACTED]`.

| Ordem | Diretório | Comando sanitizado | Finalidade | Saída | Resultado |
|---:|---|---|---|---:|---|
| 1 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `Get-Location; git branch --show-current; git rev-parse HEAD; git status --short; git remote -v` | Confirmar baseline do repositório | 0 | Branch, HEAD e árvore inicial conferidos; sem `origin`. |
| 2 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `foreach (...) { Get-Content ... }` | Ler roadmaps, decisões e registros das fases anteriores | 0 | Documentação obrigatória consultada; saída extensa foi capturada pelo executor. |
| 3 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `Get-ChildItem ...; java -version; Set-Location backend; $env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd --version` | Inspecionar estrutura e verificar JVM/Maven efetivos | 0 | Wrapper executado com Java 17.0.16 e Maven 3.9.16. |
| 4 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `docker ps -a; docker volume ls` | Inspecionar containers e volumes sem alterá-los | 0 | Serviços parados e volumes preservados. |
| 5 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `Get-ChildItem backend/src/main/resources/db/migration; Get-ChildItem backend/src/main/java/...` | Conferir migrations e módulos | 0 | V1–V6 e módulos existentes identificados. |
| 6 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `...` | Inspeção inicial de arquivos por caminho incorreto | 1 | Falha de caminho; os arquivos reais usam o pacote `com.perfumestore.backend`; nenhuma alteração foi feita. |

As próximas entradas serão acrescentadas imediatamente após cada comando real, incluindo falhas e reexecuções.
## Continuação — comandos realmente executados

Os comandos abaixo completam o registro cronológico. Edições de código e documentação foram feitas com `apply_patch`; não houve comando PowerShell de escrita equivalente. Os logs longos ficaram no diretório ignorado `Docs/execucoes/logs/`.

| Ordem | Diretório | Comando sanitizado | Finalidade | Código | Resultado |
|---:|---|---|---|---:|---|
| 7 | `...\backend` | `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd -q -DskipTests compile` | Compilar após a implementação inicial | 1 | Falhou porque o mapeamento de `OrderRecord` não passava todos os argumentos. |
| 8 | `...\backend` | `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd -q -DskipTests compile` | Recompilar após correção do construtor | 0 | Compilação aprovada. |
| 9 | `...\backend` | `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd test` | Primeira regressão | 1 | 58 testes; falha porque `CatalogIntegrationTest` ainda esperava V1–V6. |
| 10 | `...\backend` | `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd '-Dtest=OrderIntegrationTest,CatalogIntegrationTest' test` | Foco após ajustar o teste de migration | 0 | 10 testes aprovados. |
| 11 | `...\backend` | `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd test` | Regressão após adicionar concorrência | 1 | 64 erros de inicialização; scheduler SpEL não encontrou `orderProperties`. |
| 12 | `...\backend` | `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; $log='target/phase8-full-test-rerun.log'; .\mvnw.cmd test *> $log; $code=$LASTEXITCODE; Write-Output "MAVEN_EXIT=$code"; Get-Content $log | Select-Object -Last 35; exit $code` | Regressão após corrigir `@Scheduled` | 0 | 64 testes, 0 falhas, 0 erros, 0 ignorados. |
| 13 | `...\backend` | `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; $log='target/phase8-clean-verify.log'; .\mvnw.cmd clean verify *> $log; $code=$LASTEXITCODE; ...; exit $code` | Primeiro build completo | 1 | Falhou ao tentar apagar o log que estava dentro de `target` durante `clean`. |
| 14 | `...\backend` | `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; New-Item -ItemType Directory -Force Docs/execucoes/logs | Out-Null; $log='..\Docs\execucoes\logs\phase8-clean-verify.log'; .\mvnw.cmd clean verify *> $log; $code=$LASTEXITCODE; Write-Output "MAVEN_EXIT=$code"; Get-Content $log | Select-Object -Last 45; exit $code` | Build final completo | 0 | 64 testes aprovados; BUILD SUCCESS; JAR executável gerado. |
| 15 | `...` | `Get-NetTCPConnection -State Listen -LocalPort 55432,18088 ...; docker ps ...` | Conferir portas e nomes isolados | 0 | Sem conflito. |
| 16 | `...` | `docker run -d --name perfumestore-phase8-postgres -e POSTGRES_DB=perfumestore_phase8 -e POSTGRES_USER=phase8_user -e POSTGRES_PASSWORD=[REDACTED] -p 55432:5432 -v perfumestore-phase8-data:/var/lib/postgresql/data postgres:18.6` | Criar PostgreSQL isolado | 0 | Container criado, mas iniciou com exit 1 por layout de volume da imagem 18.6. |
| 17 | `...` | `$ready=$false; for (...) { docker exec perfumestore-phase8-postgres pg_isready ... }; ...` | Aguardar o primeiro PostgreSQL | N/A | Não ficou pronto; logs confirmaram a incompatibilidade de volume. |
| 18 | `...` | `docker run -d --name perfumestore-phase8-postgres-v2 -e POSTGRES_DB=perfumestore_phase8 -e POSTGRES_USER=phase8_user -e POSTGRES_PASSWORD=[REDACTED] -p 55432:5432 -v perfumestore-phase8-data-v2:/var/lib/postgresql postgres:18.6` | Criar PostgreSQL corrigido | 0 | Container isolado em execução. |
| 19 | `...` | `$ready=$false; for (...) { docker exec perfumestore-phase8-postgres-v2 pg_isready ... }; docker inspect ...` | Aguardar banco e confirmar destino | 0 | PostgreSQL 18.6 pronto, volume v2 confirmado. |
| 20 | `...` | `$env:JAVA_HOME=...; $java17=...; $env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:55432/perfumestore_phase8'; ...; & java -jar ...` | Testar provisionamento dedicado com comando Java do PATH | 1 | Java 8 rejeitou JAR compilado para class file 61. |
| 21 | `...` | `$env:JAVA_HOME=...; $java17=...; ...; & $java17 -jar ... --spring.profiles.active=local,provision-admin` | Provisionamento com JDK17 e senha sanitizada curta | 1 | Política de senha rejeitou entrada curta; nenhum ADMIN foi criado. |
| 22 | `...` | `Get-Content ...PasswordPolicy.java; ...` | Conferir limite de senha | 0 | Limite mínimo confirmado em 12 caracteres. |
| 23 | `...` | `$env:JAVA_HOME=...; $java17=...; ...; & $java17 -jar ... --spring.profiles.active=local,provision-admin` | Provisionar ADMIN sintético com variáveis externas | 0 | ADMIN criado; Flyway validou V1–V7; processo encerrou. |
| 24 | `...` | `Get-ChildItem ...migration; docker exec ... psql ... flyway_schema_history ...` | Conferir migrations e ADMIN | 0 | V1–V7 aplicadas; um ADMIN sintético. |
| 25 | `...` | `$sql="insert ... categories ... products ... product_presentations ... product_images ... inventory_balances ..."; docker exec perfumestore-phase8-postgres-v2 psql -U phase8_user -d perfumestore_phase8 -v ON_ERROR_STOP=1 -c $sql` | Criar fixture comercial sintética somente no banco isolado | 0 | Categoria, produto, apresentação, imagem primária e estoque físico 3 criados. |
| 26 | `...` | `docker ps ...; Get-Content .gitignore; Get-ChildItem backend/target -Filter '*.jar'` | Conferir artefato e exclusões | 0 | JAR final presente; logs/target/data ignorados. |
| 27 | `...` | `& .\Docs\execucoes\logs\phase8-smoke.ps1 | Tee-Object ...` | Smoke HTTP inicial | 1 | Health 503 por SMTP local indisponível; corrigido com Mailpit/indicador opcional. |
| 28 | `...` | `docker run -d --name perfumestore-phase8-mailpit -p 51025:1025 -p 58025:8025 axllent/mailpit:v1.27.8` | Iniciar Mailpit isolado | 0 | Mailpit em execução. |
| 29 | `...` | `docker port perfumestore-phase8-mailpit; docker inspect ...; docker logs ...` | Confirmar SMTP/UI | 0 | Portas 51025/58025 confirmadas. |
| 30 | `...` | `& $java17 -jar backend/target/perfumestore-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local` com `MAIL_PORT=51025` e `MANAGEMENT_HEALTH_MAIL_ENABLED=false` | Iniciar JAR final para smoke | 0 | Aplicação iniciou em 18088; Flyway sem nova migration. |
| 31 | `...` | `Start-Sleep -Seconds 25; Invoke-WebRequest .../actuator/health ...` | Aguardar health completo | 0 | HTTP 200 após a inicialização. |
| 32 | `...` | `$env:PHASE8_CUSTOMER_PASSWORD=[REDACTED]; & .\Docs\execucoes\logs\phase8-smoke.ps1 | Tee-Object ...` | Smoke HTTP completo | 0 | Claim, pedido 70,90 BRL, replay idempotente, aprovação simulada, carrinho vazio. |
| 33 | `...` | `$s=New-Object ...WebRequestSession; ... POST /api/v1/cart/items ...; ...GetCookies(.../api/v1/cart)` | Confirmar cookie visitante fora do escopo raiz | 0 | `CART_TOKEN` presente. |
| 34 | `...` | `$env:PHASE8_CUSTOMER_PASSWORD=[REDACTED]; & .\Docs\execucoes\logs\phase8-smoke.ps1` | Repetir com mesma chave | 1 | HTTP 409 esperado porque a mesma chave recebeu quoteId diferente. |
| 35 | `...` | `docker exec ... psql ... select orders, inventory_balances, stock_reservations, inventory_movements` | Conferir efeitos finais | 0 | Pedido CONFIRMED/APPROVED; físico 2, reservado 0, uma confirmação. |
| 36 | `...` | `... sessão anônima POST checkout ...; ... sessão CUSTOMER POST endereço sem X-XSRF-TOKEN ...` | Smoke negativo de autenticação/CSRF | 0 | HTTP 401 e HTTP 403. |
| 37 | `...` | `... iniciar JAR final; ... login; GET /api/v1/me/orders; GET /api/v1/cart ...` | Reiniciar aplicação e verificar persistência | 0 | Health 200, pedido 1 e carrinho persistido. |
| 38 | `...` | `docker stop perfumestore-phase8-mailpit perfumestore-phase8-postgres-v2 perfumestore-phase8-postgres` | Encerrar somente recursos da homologação | 0 | Containers parados; volumes preservados. |
| 39 | `...` | `git status --short; git diff --stat; git diff --name-only; git ls-files --others --exclude-standard` | Revisar escopo e arquivos | 0 | Somente arquivos da Fase 8 alterados/criados; logs ignorados. |
| 40 | `...` | `git diff --check` | Verificar whitespace | 0 | Sem erros de diff. |

Observações: a tentativa de iniciar o JAR com `Start-Process` foi bloqueada pelo executor antes de executar por política de processo em segundo plano; o smoke foi realizado em sessão TTY dedicada. Um comando de busca com glob PowerShell inválido também foi tentado e retornou exit 1; não alterou arquivos. Onde o executor não preservou o texto integral de blocos longos (`...`), o log local correspondente contém a saída real e a limitação está registrada aqui, sem reconstruir segredos.

| 41 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `git add Docs/STATUS.md Docs/backend/... README.md backend/src/main/... backend/src/resources/... backend/src/test/...; git status --short; git diff --cached --stat; git diff --cached --check` | Stage explícito e revisão antes do commit | 0 | 40 arquivos da fase staged; sem erro de diff. |
| 42 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `git commit -m "feat: implement checkout and order creation"` | Criar o commit local solicitado | 0 | Commit criado; inicialmente `d012c2651122709942163098df4c629d04641fd9`. |
| 43 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `git status --short; git branch --show-current; git rev-parse HEAD; git log -1 --pretty=fuller; git remote -v; docker ps -a --filter name=perfumestore-phase8 ...; docker volume ls ...; Get-ChildItem backend/target -Filter '*.jar'` | Verificação final de Git, recursos e artefato | 0 | Árvore limpa, branch main, sem remote, containers parados, volumes preservados e JAR presente. |
| 44 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `git add Docs/execucoes/FASE_08_RELATORIO.md Docs/execucoes/FASE_08_COMANDOS.md; git commit --amend --no-edit` | Atualizar os registros com o fechamento e o próprio comando de commit | 0 | Commit final mantém a mensagem solicitada e inclui o estado final dos registros. |
| 45 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `docker start perfumestore-phase8-postgres-v2 perfumestore-phase8-mailpit; docker ps --filter name=perfumestore-phase8 ...` | Reutilizar somente os recursos isolados para verificar OpenAPI | 0 | Containers isolados iniciados; desenvolvimento não tocado. |
| 46 | `...` | `& $java17 -jar backend/target/perfumestore-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local` com datasource isolado, `SERVER_PORT=18088`, `MANAGEMENT_HEALTH_MAIL_ENABLED=false` | Subir o JAR final para a verificação OpenAPI | 0 | Aplicação iniciou e Flyway informou schema v7 atualizado. |
| 47 | `...` | `Start-Sleep -Seconds 25; $h=curl.exe .../actuator/health; $a=curl.exe .../v3/api-docs; ...` | Verificar health e caminhos publicados | 0 | OpenAPI com 38.429 bytes, 0 caminhos ausentes; liveness/readiness UP. |
| 48 | `...` | `docker stop perfumestore-phase8-mailpit perfumestore-phase8-postgres-v2; docker ps -a ...; docker volume ls ...` | Encerrar novamente os recursos isolados | 0 | Containers parados e volumes preservados. |
| 49 | `...` | `git add Docs/execucoes/FASE_08_RELATORIO.md Docs/execucoes/FASE_08_TESTES.md Docs/execucoes/FASE_08_COMANDOS.md; git commit --amend --no-edit` | Registrar a verificação OpenAPI e o encerramento final | 0 | Commit final atualizado. |
| 50 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `git status --porcelain; git diff --check; Write-Output "FINAL_HEAD=$(git rev-parse HEAD)"; git log -1 --oneline; docker ps --filter name=perfumestore-phase8 ...; git remote -v` | Verificação final após o último amend | 0 | Árvore limpa, diff sem erros, commit final conferido, containers parados e sem remote. |
