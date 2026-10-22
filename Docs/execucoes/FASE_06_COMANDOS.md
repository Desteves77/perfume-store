# Fase 6 — Comandos executados

Este arquivo é atualizado durante a execução. Segredos, senhas, cookies e tokens são substituídos por `[REDACTED]` ou não são registrados.

## Convenções

- Diretório raiz: `C:\Users\User\Desktop\Ecommerce PerfumeStore`.
- Diretório do backend: `C:\Users\User\Desktop\Ecommerce PerfumeStore\backend`.
- Saídas longas de Maven e smoke ficam em `Docs/execucoes/logs/`, diretório ignorado pelo Git.
- Quando o executor não expuser um código de saída, isso será indicado explicitamente, sem reconstrução fictícia.

## Execução inicial

| Ordem | Diretório | Comando | Finalidade | Código | Resultado |
|---:|---|---|---|---:|---|
| 1 | raiz | `Get-Location; git branch --show-current; git rev-parse HEAD; git status --short --branch; git ls-files --others --exclude-standard; git remote -v` | Registrar diretório, Git, HEAD, árvore, não rastreados e remotes. | 0 | Estado inicial registrado no relatório. |
| 2 | raiz | `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; java -version; .\backend\mvnw.cmd -version` | Confirmar JVM e Maven Wrapper. | 0 | Java 17.0.16 e Maven 3.9.16. |
| 3 | raiz | `docker version; docker ps --all; docker volume ls` | Conferir Docker, containers e volumes sem alterá-los. | 0 | Docker Desktop disponível; recursos existentes preservados. |
| 4 | raiz | `rg --files -g 'AGENTS.md' ...; Get-ChildItem ...` | Localizar instruções e estrutura real. | 0 | Nenhum AGENTS.md encontrado; módulos e migrations V1–V4 identificados. |
| 5 | raiz | `Get-Content` dos roadmaps, status, backend e registros da Fase 5 | Ler documentação obrigatória. | 0 | Fase 6 e lacunas comerciais confirmadas. |

## Implementação, testes e homologação

Os comandos adicionais serão registrados em ordem abaixo durante a execução, com suas saídas sanitizadas e logs locais quando aplicável.

## Registro consolidado da implementação e homologação

Os comandos de shell abaixo foram executados no diretório indicado. Edições de código feitas por patch interno estão registradas nos relatórios de implementação e não são comandos de shell; não foram convertidas em comandos fictícios.

| Ordem | Diretório | Comando sanitizado | Finalidade | Código | Resultado |
|---:|---|---|---|---:|---|
| 6 | raiz | $env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; java -version; .\backend\mvnw.cmd -version | Confirmar JVM e Wrapper | 0 | Java 17.0.16; Maven 3.9.16 |
| 7 | raiz | docker version; docker ps --all; docker volume ls | Conferir Docker, containers e volumes | 0 | Docker disponível; volumes preservados |
| 8 | backend | .\mvnw.cmd --batch-mode -DskipTests compile | Compilar código da fase | 1 | Falhou no import inicial do serializador decimal |
| 9 | backend | .\mvnw.cmd --batch-mode -DskipTests compile | Revalidar após corrigir import | 0 | Compilação passou |
| 10 | backend | .\mvnw.cmd --batch-mode test-compile | Compilar testes | 1 | Falhou na sintaxe de text block |
| 11 | backend | .\mvnw.cmd --batch-mode test-compile | Revalidar após ajustar corpos JSON | 1 | Ainda havia problema de UTF-8/escape |
| 12 | backend | .\mvnw.cmd --batch-mode test-compile | Revalidar após normalizar o teste | 1 | Helper de teste ocultava método user |
| 13 | backend | .\mvnw.cmd --batch-mode test-compile | Revalidar após renomear helper | 0 | Compilação de testes passou |
| 14 | backend | .\mvnw.cmd --batch-mode -Dtest=AddressShippingIntegrationTest test | Executar testes da fase | 1 | Primeira expectativa de 401/403 incompatível com ordem CSRF |
| 15 | backend | .\mvnw.cmd --batch-mode -Dtest=AddressShippingIntegrationTest test | Reexecutar após corrigir expectativa | 0 | 8 testes passaram |
| 16 | raiz | Copy-Item phase6-acceptance.env.example .env.phase6; docker compose -p perfumestore-phase6 -f compose.phase6-acceptance.yaml --env-file .env.phase6 config --quiet | Criar configuração local ignorada e validar Compose | 0 | Configuração válida |
| 17 | raiz | docker compose -p perfumestore-phase6 -f compose.phase6-acceptance.yaml --env-file .env.phase6 up -d postgres mailpit; docker compose -p perfumestore-phase6 -f compose.phase6-acceptance.yaml --env-file .env.phase6 ps | Iniciar banco/Mailpit isolados | 0 | Serviços iniciados e saudáveis |
| 18 | backend | .\mvnw.cmd --batch-mode clean verify | Regressão completa e artefato | 0 | 49 testes, 0/0/0; JAR gerado |
| 19 | raiz | powershell.exe -NoProfile -File Docs/execucoes/logs/start-phase6-app.ps1 | Iniciar app pela sonda inicial | não exposto; interrompido | A sonda ficou bloqueada em resposta chunked; app chegou a iniciar, mas esse caminho foi abandonado |
| 20 | raiz | Get-Process java; Get-Content Docs/execucoes/logs/phase6-app-01.log; docker compose -p perfumestore-phase6 -f compose.phase6-acceptance.yaml --env-file .env.phase6 ps | Diagnosticar o launcher e serviços | 0 | Startup completo no log; serviços saudáveis |
| 21 | raiz | Invoke-WebRequest http://127.0.0.1:18280/actuator/health | Testar health pelo cliente PowerShell | 0 do wrapper de captura; resultado HTTP não retornou no timeout | Timeout de leitura; não marcado como smoke aprovado |
| 22 | raiz | curl.exe -v --max-time 30 http://127.0.0.1:18280/actuator/health | Confirmar health com cliente HTTP simples | 0 | HTTP 200 / status UP |
| 23 | raiz | controle de término da sessão do launcher | Encerrar processo iniciado pelo launcher | 1 | Processo encerrado; sem alteração persistida |
| 24 | raiz | powershell.exe -NoProfile -File Docs/execucoes/logs/phase6-http-smoke.ps1 | Smoke contra app parada | 1 | Falha esperada: conexão recusada |
| 25 | raiz | carregamento de .env.phase6; Start-Process java.exe -jar backend\target\perfumestore-backend-0.0.1-SNAPSHOT.jar com logs phase6-app-02 e PID | Iniciar app pelo JAR com PID controlado | 0 | PID 4084 iniciado |
| 26 | raiz | Start-Sleep -Seconds 20; curl.exe --silent --show-error --max-time 15 http://127.0.0.1:18280/actuator/health | Aguardar e verificar health | 0 | Health UP |
| 27 | raiz | powershell.exe -NoProfile -File Docs/execucoes/logs/phase6-http-smoke.ps1 | Smoke inicial | 1 | 19 asserções, falha no cenário sem CSRF e contador incorreto |
| 28 | raiz | Get-Content Docs/execucoes/logs/phase6-smoke-results.json | Inspecionar resultados sanitizados | 0 | Identificada retenção de header CSRF na sessão |
| 29 | raiz | edição do script de smoke para header vazio e contagem | Corrigir teste operacional | 0 | Script ajustado |
| 30 | raiz | powershell.exe -NoProfile -File Docs/execucoes/logs/phase6-http-smoke.ps1 | Reexecutar smoke | 1 | Ainda havia comportamento de header na sessão |
| 31 | raiz | Get-Content Docs/execucoes/logs/phase6-smoke-results.json | Diagnosticar segunda execução | 0 | Cenário sem CSRF ainda recebia 200 |
| 32 | raiz | edição do script de smoke para envio explícito de header vazio e contador por objetos | Correção final do smoke | 0 | Script pronto |
| 33 | raiz | powershell.exe -NoProfile -File Docs/execucoes/logs/phase6-http-smoke.ps1 | Smoke final | 0 | 19 de 19 aprovados; UUIDs sintéticos registrados no log ignorado |
| 34 | raiz | $sql='select count(*) ...'; $sql | docker compose -p perfumestore-phase6 -f compose.phase6-acceptance.yaml --env-file .env.phase6 exec -T postgres psql -U phase6_user -d perfumestore_phase6 -t -A | Registrar estado antes do restart | 0 | 6 endereços ativos, 8 cotações, Flyway 5 |
| 35 | raiz | Stop-Process PID 4084; docker compose -p perfumestore-phase6 -f compose.phase6-acceptance.yaml --env-file .env.phase6 stop postgres mailpit; docker compose ... ps -a | Parar somente recursos isolados | 0 | App e containers parados; volume preservado |
| 36 | raiz | docker compose -p perfumestore-phase6 -f compose.phase6-acceptance.yaml --env-file .env.phase6 start postgres mailpit; docker compose ... ps | Reiniciar banco/Mailpit | 0 | Containers saudáveis |
| 37 | raiz | Start-Process java.exe -jar ... phase6-app-03.log com PID salvo | Reiniciar aplicação a partir do JAR | 0 | PID 2360 iniciado |
| 38 | raiz | Start-Sleep -Seconds 20; curl.exe --silent --show-error --max-time 15 http://127.0.0.1:18280/actuator/health | Health logo após restart | 7 | Falhou por iniciar a sonda antes do término do startup; não indica falha da aplicação |
| 39 | raiz | Get-Content phase6-app-03.log; Get-Process java | Diagnosticar startup após restart | 0 | App subiu; Flyway validou 6 migrations, schema V5 atualizado |
| 40 | raiz | curl.exe --silent --show-error --max-time 15 http://127.0.0.1:18280/actuator/health | Health após startup completo | 0 | Status UP |
| 41 | raiz | consulta psql de contagens no Compose isolado | Confirmar persistência após restart | 0 | 6 endereços ativos, 8 cotações, Flyway 5 |
| 42 | raiz | Stop-Process PID 2360; docker compose -p perfumestore-phase6 -f compose.phase6-acceptance.yaml --env-file .env.phase6 stop postgres mailpit; docker compose ... ps -a | Encerrar recursos da fase | 0 | Containers Exited(0), volume preservado |
| 43 | raiz | docker ps --all --format 'table ...'; docker volume ls --format '{{.Name}}' | Conferir recursos finais e volume | 0 | Phase6 parado; dev e volumes preservados |

A leitura inicial agrupada dos documentos foi executada antes da implementação, mas o executor não preservou no log interno cada argumento do comando de leitura; o registro original acima informa a finalidade e os arquivos obrigatórios lidos sem reconstruir uma linha de comando fictícia. Os comandos de patch/edição de fonte e documentação também não têm código de shell separado; suas alterações estão refletidas no diff e nos relatórios.

## Verificação final de documentação e Git

Os comandos finais de OpenAPI, diff, staged e commit serão adicionados após a revisão final.

| 44 | raiz | docker compose -p perfumestore-phase6 -f compose.phase6-acceptance.yaml --env-file .env.phase6 start postgres mailpit | Levantar o ambiente isolado para verificar o artefato final e OpenAPI | 0 | Serviços iniciados |
| 45 | raiz | carregamento de .env.phase6; Start-Process java.exe -jar backend\target\perfumestore-backend-0.0.1-SNAPSHOT.jar com logs phase6-app-04 e PID salvo | Iniciar o JAR final | 0 | PID 12332 |
| 46 | raiz | loop de health com curl.exe em http://127.0.0.1:18280/actuator/health | Aguardar startup | não exposto no primeiro retorno; verificação posterior 0 | O primeiro polling ultrapassou a janela do executor; a aplicação continuou subindo |
| 47 | raiz | Get-Process java; Get-Content Docs\execucoes\logs\phase6-app-04.log -Tail 60; curl.exe --silent --show-error --max-time 15 http://127.0.0.1:18280/actuator/health | Confirmar startup completo | 0 | Health UP |
| 48 | raiz | $docText=curl.exe --silent --show-error --max-time 20 http://127.0.0.1:18280/v3/api-docs; ConvertFrom-Json; teste dos caminhos | Verificar OpenAPI real do JAR | 0 | 31 caminhos; me/addresses=True; shipping/quotes=True |
| 49 | raiz | Stop-Process PID 12332; docker compose -p perfumestore-phase6 -f compose.phase6-acceptance.yaml --env-file .env.phase6 stop postgres mailpit; docker compose ... ps -a | Encerrar recursos da verificação final | 0 | App e containers parados; volume preservado |
| 50 | raiz | docker ps --all --format 'table ...'; docker volume ls --format '{{.Name}}' | Confirmar que dev e homologações anteriores permanecem preservados | 0 | Containers existentes não foram removidos; volume phase6 permanece |
