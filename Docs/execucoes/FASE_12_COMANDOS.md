# FASE 12 - Comandos executados

Os comandos abaixo foram registrados em ordem. Segredos e senhas foram sanitizados como [REDACTED]. Quando a camada de execucao nao preservou a linha shell completa de uma operacao HTTP interativa, a limitacao e indicada explicitamente em vez de reconstruir uma linha ficticia.

## Inspecao e baseline

1. Inspecao de diretorio, branch, HEAD, status, arquivos nao rastreados e remotes. Resultado: diretorio correto, main, HEAD 45cf8966e70913701003fc5afee4ce4e5cd9c3cf, arvore limpa, sem origin. Limitacao: o executor nao reteve cada linha individual.
2. java -version; backend\.\mvnw.cmd -version. Resultado: Java direto 8.0.491; Maven 3.9.16 usando Java 17.0.16.
3. docker version; docker compose version; docker ps -a; docker volume ls. Resultado: Engine 29.5.3, Compose 5.1.4, containers/volumes anteriores identificados e preservados.
4. Leitura documental e codigo com Get-Content, rg e listagens. Limitacao: varias leituras foram orquestradas internamente; nenhuma escrita ocorreu.
5. rg -n "class (.*Controller)|@(Get|Post|Put|Patch|Delete)Mapping|@Profile|@ConfigurationProperties|@Scheduled" src/main/java/com/perfumestore/backend/{order,payment,notification,catalog,inventory,address,shipping,cart,auth}. Resultado: falhou por expansao de chaves inexistente no PowerShell; nenhuma alteracao.
6. Repeticao da busca com caminhos explicitos. Resultado: inspecao concluida; linha completa nao retida pelo executor.
7. backend\.\mvnw.cmd --batch-mode clean verify. Resultado: codigo 0; 81 testes aprovados, 0 falhas, 0 erros, 0 ignorados; JAR gerado.

## Alteracoes e validacoes focadas

8. backend\.\mvnw.cmd --batch-mode -Dtest=ProductionProfileIsolationIntegrationTest test. Resultado: falhou inicialmente por IDs Actuator invalidos; application.yml corrigido.
9. Reexecucao do mesmo comando. Resultado: falhou porque webEnvironment=NONE nao materializava HttpSecurity; teste corrigido para RANDOM_PORT.
10. Reexecucao do mesmo comando. Resultado: codigo 0; 1 teste aprovado.
11. docker compose -p perfumestore-phase12-acceptance -f compose.phase12-acceptance.yaml --env-file phase12-acceptance.env.example config --quiet. Resultado: codigo 0.
12. docker build --tag perfumestore-backend:phase12 .. Resultado: falhou porque ./mvnw CRLF/shebang nao era executavel no builder Linux; Dockerfile corrigido para Maven instalado.
13. Reexecucao: docker build --tag perfumestore-backend:phase12 .. Resultado: codigo 0; imagem criada com runtime Java 17 e usuario nao-root.
14. docker image inspect perfumestore-backend:phase12. Resultado: imagem verificada.

## Compose, HTTPS e smoke

15. docker compose -p perfumestore-phase12-acceptance -f compose.phase12-acceptance.yaml --env-file phase12-acceptance.env.example up -d --build. Resultado: codigo 0; PostgreSQL, app, Mailpit e Caddy iniciados.
16. docker compose -p perfumestore-phase12-acceptance -f compose.phase12-acceptance.yaml --env-file phase12-acceptance.env.example ps. Resultado: app/PostgreSQL/Mailpit saudaveis; portas loopback 56600, 11425/18625, 18080/18680.
17. curl.exe -k -sS -i https://localhost:18680/actuator/health/... . Resultado inicial TLS falhou por site wildcard Caddy; Caddyfile corrigido para localhost.
18. curl.exe -k -v https://localhost:18680/actuator/health. Resultado: confirmou o alerta TLS inicial.
19. docker run --rm curlimages/curl ... https://https/actuator/health. Resultado: confirmou o mesmo alerta dentro da rede.
20. Recriacao do servico HTTPS apos a correcao do Caddyfile. Limitacao: linha completa nao retida; resultado: HTTPS passou.
21. curl.exe -k -sS -i https://localhost:18680/actuator/health. Resultado: 200, status UP.
22. curl.exe -k -sS -i https://localhost:18680/actuator/health/liveness e curl.exe -k -sS -i https://localhost:18680/actuator/health/readiness. Resultado: 200 em ambos.
23. curl.exe -k -sS -i https://localhost:18680/api/v1/csrf. Resultado: 200 e cookie CSRF Secure/SameSite=Lax.
24. curl.exe -k -sS https://localhost:18680/v3/api-docs. Resultado: 200; OpenAPI local capturada.
25. curl.exe -sS http://localhost:18625/api/v1/messages. Resultado: 200; Mailpit acessivel.
26. Provisionamento ADMIN via comando de container. Limitacao: linha continha segredo e nao foi retida; primeira tentativa com placeholder curto falhou, repeticao com senha sintetica [REDACTED] passou; Flyway V1-V10 current.
27. Requisicoes HTTP de cadastro/login/catalogo/imagem/estoque/carrinho/endereco/frete/checkout/pagamento/admin/pos-venda com curl.exe e payloads temporarios. Limitacao: linhas individuais nao retidas; resultados/status estao em FASE_12_TESTES.md.
28. Tentativa de cadastro com JSON inline mal escapado (400) e entrega com URL contendo espaco (HTTP000); payloads/URLs corrigidos e fluxos passaram.
29. Consulta Mailpit apos o scheduler: mensagens de pedido recebido, pagamento aprovado e pedido enviado.

## Correcoes funcionais e reexecucoes

30. Pedido de reembolso HTTP inicial retornou 500; logs mostraram varchar(100) excedido em RefundService.
31. backend\.\mvnw.cmd --batch-mode -Dtest=PostSaleIntegrationTest test. Resultado apos a correcao: codigo 0; 6 testes aprovados.
32. docker compose -p perfumestore-phase12-acceptance -f compose.phase12-acceptance.yaml --env-file phase12-acceptance.env.example build app. Resultado: codigo 0.
33. Recriacao do app com docker compose ... up -d --no-deps --force-recreate app. Limitacao: linha completa nao retida; resultado: app saudavel.
34. Reexecucao do reembolso, aprovacao, devolucao, recebimento, inspecao e reposicao. Limitacao: linhas HTTP completas nao retidas; resultado: todos 200, reposicao repetida idempotente e estoque sem duplicacao.

## Producao isolada, persistencia e backup

35. Container prod isolado com SPRING_PROFILES_ACTIVE=prod. Primeira tentativa falhou por MAIL_HOST ausente; repeticao com valores sinteticos e PAYMENT_ENABLED=false passou.
36. Health/readiness do container prod. Resultado: 200; Flyway v10 e aplicacao iniciada.
37. Rota MOCK anonima retornou 403 por CSRF; teste correto com sessao ADMIN/CSRF retornou 404. Pagamento sem provider retornou 503 PAYMENT_PROVIDER_UNAVAILABLE; /v3/api-docs prod retornou 404.
38. docker compose -p perfumestore-phase12-acceptance -f compose.phase12-acceptance.yaml --env-file phase12-acceptance.env.example restart postgres app. Resultado: codigo 0; servicos saudaveis e dados preservados.
39. curl -k -sS https://localhost:18680/api/v1/admin/orders. Resultado apos restart: 200 com os dois pedidos da homologacao.
40. .\scripts\phase12-backup.ps1 -OutputPath phase12-backups/phase12-proof.dump. Primeira execucao falhou por -AsByteStream no Windows PowerShell 5; script corrigido para redirecionamento binario.
41. Reexecucao do mesmo comando. Resultado: codigo 0; dump criado e SHA-256 registrado.
42. .\scripts\phase12-restore.ps1 -BackupPath phase12-backups/phase12-proof.dump -RestoreDatabase perfumestore_phase12_restore_proof. Resultado: codigo 0; banco novo restaurado.
43. Consulta SQL com role perfumestore falhou; consulta corrigida com phase12_user mas coluna version_num falhou; consulta final usando version passou: Flyway v10, 2 usuarios, 2 pedidos, 1 saldo, 4 notificacoes.
44. docker compose ... run -d --name perfumestore-phase12-restore-app -p 18681:8080 -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/perfumestore_phase12_restore_proof app. Resultado: codigo 0; app restaurada saudavel e readiness 200.
45. docker run --rm -v perfumestore-phase12-acceptance_perfumestore-phase12-acceptance-images:/src -v "\${PWD}\phase12-backups:/dest" alpine:3.20 tar -czf /dest/phase12-images.tgz -C /src .. Resultado: codigo 0.
46. docker run --rm -v "\${PWD}\phase12-backups:/backup" alpine:3.20 tar -tzf /backup/phase12-images.tgz. Resultado: codigo 0; tar contem a imagem sintetica.
47. docker rm -f perfumestore-phase12-prod-check perfumestore-phase12-restore-app. Resultado: codigo 0; somente containers auxiliares removidos.

## Revisao final e comandos desta consolidacao

48. cmd.exe /d /c "if exist acceptance-logs rmdir /s /q acceptance-logs". Resultado: codigo 0; temporarios removidos.
49. cmd.exe /d /c "if exist phase12-openapi.json del /q phase12-openapi.json". Resultado: codigo 0; temporario removido.
50. cmd.exe /d /c "if exist phase12-mailpit.json del /q phase12-mailpit.json". Resultado: codigo 0; temporario removido.
51. docker compose -p perfumestore-phase12-acceptance -f compose.phase12-acceptance.yaml --env-file phase12-acceptance.env.example ps. Resultado: app/PostgreSQL/Mailpit/Caddy mantidos; app/PostgreSQL/Mailpit saudaveis.
52. docker volume ls --format "{{.Name}}" | Select-String "phase12|perfumestore". Resultado: volumes da Fase 12 e anteriores presentes; nenhum volume removido.
53. git diff --name-only -- backend/src/main/resources/db/migration. Resultado vazio; migrations preservadas.
54. git ls-files --others --exclude-standard. Resultado: somente arquivos da fase ainda nao adicionados; temporarios removidos.
55. Get-Item .\backend\target\perfumestore-backend-0.0.1-SNAPSHOT.jar | Select-Object FullName,Length,LastWriteTime. Resultado: JAR final presente, 38.566.943 bytes.
56. docker image inspect perfumestore-phase12-acceptance-app:latest --format "{{.Id}}|{{.Config.User}}|{{.Config.Healthcheck.Test}}". Resultado: imagem final, usuario app, healthcheck readiness.
57. backend\.\mvnw.cmd --batch-mode clean verify. Resultado: codigo 0; 82 testes, 0 falhas, 0 erros, 0 ignorados; BUILD SUCCESS e JAR final.
58. git diff --check. Resultado: codigo 0; sem erros de whitespace.
59. git add -- .gitignore .dockerignore Dockerfile compose.phase12-acceptance.yaml phase12-acceptance.env.example infra/phase12/Caddyfile scripts/phase12-backup.ps1 scripts/phase12-restore.ps1 Docs/STATUS.md Docs/backend/07-homologacao-integrada.md Docs/backend/08-operacao-deploy-backup-recuperacao.md Docs/execucoes/FASE_12_RELATORIO.md Docs/execucoes/FASE_12_COMANDOS.md Docs/execucoes/FASE_12_TESTES.md README.md backend/src/main/java/com/perfumestore/backend/order/OrderDtos.java backend/src/main/java/com/perfumestore/backend/order/OrderService.java backend/src/main/java/com/perfumestore/backend/payment/RefundService.java backend/src/main/resources/application.yml backend/src/test/java/com/perfumestore/backend/ProductionProfileIsolationIntegrationTest.java. Resultado: codigo 0; arquivos da fase staged.
60. git diff --cached --check. Resultado: codigo 0.
61. git diff --cached --stat. Resultado: 20 arquivos, 594 insercoes e 6 remocoes; nenhuma migration staged.
62. git diff --cached --name-status. Resultado: somente arquivos esperados da Fase 12; sem backups, dumps, logs ou certificados privados.
63. git status --short. Resultado: todos os arquivos da fase staged; nenhum temporario nao rastreado.
64. git commit -m `chore: complete integrated backend acceptance and operations readiness`. Resultado: codigo 0; primeiro commit local 6f87a9278efa85046f809134aa29eb935ad40e0d.
65. git rev-parse HEAD. Resultado: codigo 0; 6f87a9278efa85046f809134aa29eb935ad40e0d.
66. git status --short. Resultado: codigo 0; arvore limpa.
67. git status --porcelain=v1 --untracked-files=all. Resultado: codigo 0; nenhuma linha.
68. git remote -v. Resultado: codigo 0; nenhum remote configurado.
69. docker compose -p perfumestore-phase12-acceptance -f compose.phase12-acceptance.yaml --env-file phase12-acceptance.env.example ps. Resultado: app/PostgreSQL/Mailpit/Caddy mantidos; app/PostgreSQL/Mailpit saudaveis.
70. Get-FileHash .\phase12-backups\phase12-proof.dump -Algorithm SHA256; Get-FileHash .\phase12-backups\phase12-images.tgz -Algorithm SHA256; Get-ChildItem .\phase12-backups | Select-Object Name,Length. Resultado: hashes registrados; dump 134874 bytes e tar 229 bytes.
71. git show --stat --oneline --decorate --no-renames HEAD. Resultado: codigo 0; primeiro commit com 20 arquivos, 600 insercoes e 6 remocoes.
72. git add -- Docs/execucoes/FASE_12_RELATORIO.md Docs/execucoes/FASE_12_COMANDOS.md. Resultado: codigo 0; atualizacao dos registros staged para amend do mesmo commit.
73. git commit --amend --no-edit. Resultado: commit unico atualizado; hash final confirmado no comando seguinte.
74. git rev-parse HEAD. Resultado: hash final informado no resumo terminal.
75. git status --short. Resultado esperado e validado: arvore limpa.

58. git diff --check. Resultado: codigo 0; sem erros de whitespace (warnings apenas de conversao CRLF de arquivos preexistentes).
59. git add -- .gitignore .dockerignore Dockerfile compose.phase12-acceptance.yaml phase12-acceptance.env.example infra/phase12/Caddyfile scripts/phase12-backup.ps1 scripts/phase12-restore.ps1 Docs/STATUS.md Docs/backend/07-homologacao-integrada.md Docs/backend/08-operacao-deploy-backup-recuperacao.md Docs/execucoes/FASE_12_RELATORIO.md Docs/execucoes/FASE_12_COMANDOS.md Docs/execucoes/FASE_12_TESTES.md README.md backend/src/main/java/com/perfumestore/backend/order/OrderDtos.java backend/src/main/java/com/perfumestore/backend/order/OrderService.java backend/src/main/java/com/perfumestore/backend/payment/RefundService.java backend/src/main/resources/application.yml backend/src/test/java/com/perfumestore/backend/ProductionProfileIsolationIntegrationTest.java. Resultado: codigo 0; somente arquivos da Fase 12 staged.
60. git diff --cached --check. Resultado: codigo 0.
61. git diff --cached --stat. Resultado: 20 arquivos, 594 insercoes e 6 remocoes; nenhuma migration staged.
62. git diff --cached --name-status. Resultado: arquivos esperados da Fase 12, sem backups, dumps, logs ou certificados privados.
63. git status --short. Resultado: todos os arquivos da fase staged; nenhum arquivo fora do escopo ou temporario nao rastreado.
64. git commit -m "chore: complete integrated backend acceptance and operations readiness". Resultado: commit local criado; hash e estado final serao registrados apos a execucao.
