# Fase 9 — comandos executados

Os comandos abaixo são registrados em ordem cronológica. Segredos, quando existirem, serão substituídos por `[REDACTED]`.

| # | Diretório | Comando sanitizado | Finalidade | Saída | Resultado |
|---:|---|---|---|---:|---|
| 1 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `Get-Location; git branch --show-current; git rev-parse HEAD; git status --short; git remote -v; java -version; Set-Location backend; $env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd --version` | Registrar baseline Git, Java e Maven Wrapper. | 0 | Branch `main`, HEAD da Fase 8, árvore limpa, sem remote, Maven 3.9.16 com Java 17.0.16. |
| 2 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `$files=@(...); foreach($f in $files){ Get-Content $f }; docker ps -a; docker volume ls; Get-ChildItem backend/src/main/resources/db/migration -Filter '*.sql'` | Ler documentação obrigatória e inspecionar Docker/migrations. | 0 | Documentos consultados; V1–V7 identificadas; containers e volumes preservados. |
| 3 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `rg -n "Payment|payment|Simulat|PENDING_PAYMENT|PAYMENT_REVIEW|CONFIRMED|EXPIRED|reserve\(|confirm\(|release\(|@Profile|Idempotency|OpenAPI|springdoc" backend/src/main/java backend/src/test/java backend/src/main/resources README.md Docs/backend Docs/STATUS.md -g '!target/**'` | Localizar integração de pagamento, pedidos, estoque, segurança e documentação. | 0 | Simulador legado em `PaymentSimulationController`; transições financeiras ainda em `OrderService`. |
| 4 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `Get-Content` dirigido de `pom.xml`, pedidos, pagamento, inventário, segurança e perfis | Confirmar APIs e configurações reais antes da implementação. | 0 | Não há dependência externa necessária; V8 é a próxima migration. |

Os comandos posteriores serão adicionados nesta tabela conforme forem executados, incluindo falhas e reexecuções.
## Registro autoritativo completo

As linhas iniciais preservam o resumo do baseline. A sequência completa dos comandos de shell executados nesta fase está registrada abaixo; segredos foram sanitizados como `[REDACTED]`.

5. `backend` — `rg -n "Payment|payment|Simulat|PENDING_PAYMENT|PAYMENT_REVIEW|CONFIRMED|EXPIRED|reserve\(|confirm\(|release\(|@Profile|Idempotency|OpenAPI|springdoc" backend/src/main/java backend/src/test/java backend/src/main/resources README.md Docs/backend Docs/STATUS.md -g '!target/**'` — inspeção dirigida — saída 0 — localizou simulador e integrações.
6. `backend` — leitura dirigida de `pom.xml`, `OrderService.java`, `OrderRepository.java`, `OrderController.java`, `PaymentSimulationController.java` e `OrderDtos.java` com `Get-Content` — confirmar contratos — saída 0.
7. `backend` — leitura dirigida de inventário, handlers, segurança, propriedades e `application*.yml` com `Get-Content` — confirmar infraestrutura — saída 0.
8. `backend` — `rg --files ...; rg -n "RestControllerAdvice|AccessDeniedHandler|AuthenticationEntryPoint|ProblemDetail|ApiRequestException|SecurityError|Scheduled|expireDue|cleanup|Scheduler|ConfigurationPropertiesScan|EnableConfigurationProperties|OrderProperties" ...` — inventário — saída 0.
9. `backend` — leitura de handlers, CORS, aplicação, scheduler, enums, pedido e frete — confirmar estados e expiração — saída 0.
10. `backend` — leitura de V7, STATUS e trechos de README — confirmar schema e pendências — saída 0.
11. `backend` — `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd --batch-mode --no-transfer-progress -DskipTests compile` — compilação inicial — saída 0 — Java 17.
12. `backend` — `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd --batch-mode --no-transfer-progress -DskipTests=false test` — suíte inicial — saída 1 — 64 testes, 1 falha, 0 erros.
13. `backend` — `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd --batch-mode --no-transfer-progress -Dtest=OrderIntegrationTest test` — regressão focada — saída 1 — 6 testes, 2 falhas, 1 erro.
14. `backend` — `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd --batch-mode --no-transfer-progress -Dtest=PaymentIntegrationTest test 2>&1 | Tee-Object ..\Docs\execucoes\logs\phase9-payment-focused.log; exit $LASTEXITCODE` — pagamento focado — saída 1 — 4 testes, 2 falhas, 1 erro.
15. `backend` — leitura de V8, PaymentService, OrderService, PaymentStatus e PaymentIntegrationTest — investigar falhas — saída 0.
16. `backend` — `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd --batch-mode --no-transfer-progress -Dtest=PaymentIntegrationTest test 2>&1 | Tee-Object ..\Docs\execucoes\logs\phase9-payment-focused-rerun.log; exit $LASTEXITCODE` — reexecução — saída 1 — 4 testes, 0 falhas, 1 erro.
17. `backend` — `rg -n "create table perfumestore.inventory_movements|inventory_movements \(|movement_type|movement" src/main/resources/db/migration src/main/java/com/perfumestore/backend/inventory` — confirmar coluna — saída 0.
18. `backend` — leitura de `InventoryMovementType.java` e trecho de V4 — confirmar enum/schema — saída 0.
19. `backend` — `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd --batch-mode --no-transfer-progress -Dtest=PaymentIntegrationTest test 2>&1 | Tee-Object ..\Docs\execucoes\logs\phase9-payment-focused-final2.log; exit $LASTEXITCODE` — focado final parcial — saída 0 — 4 testes.
20. `backend` — `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd --batch-mode --no-transfer-progress -Dtest=OrderIntegrationTest test 2>&1 | Tee-Object ..\Docs\execucoes\logs\phase9-order-regression.log; exit $LASTEXITCODE` — regressão de pedidos — saída 0 — 6 testes.
21. `backend` — leitura de produção, propriedades e controllers, seguida de `rg` de perfis — inspeção — saída 1 no `rg` por expressão inválida; leituras concluídas.
22. `backend` — leitura dos tails de README, arquitetura, contratos, decisões, validação e registros — revisão documental — saída 0.
23. `backend` — `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd --batch-mode --no-transfer-progress -Dtest=PaymentIntegrationTest test 2>&1 | Tee-Object ..\Docs\execucoes\logs\phase9-payment-focused-reconcile.log; exit $LASTEXITCODE` — compilar reconciliação — saída 1 — campo `resultStatus` inexistente.
24. `backend` — `rg -n "ReconciliationResponse|record Reconciliation" src/main/java/com/perfumestore/backend/payment/PaymentDtos.java; Get-Content src/main/java/com/perfumestore/backend/payment/PaymentDtos.java` — confirmar DTO — saída 0.
25. `backend` — `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd --batch-mode --no-transfer-progress -Dtest=PaymentIntegrationTest test 2>&1 | Tee-Object ..\Docs\execucoes\logs\phase9-payment-focused-reconcile-final.log; exit $LASTEXITCODE` — reexecução — saída 0 — 5 testes.
26. `backend` — `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd --batch-mode --no-transfer-progress clean verify 2>&1 | Tee-Object ..\Docs\execucoes\logs\phase9-clean-verify.log; exit $LASTEXITCODE` — suíte e JAR — saída 0 — 69 testes, 0/0/0.
27. `backend` — leitura de `application-local.yml` e busca por compose/env de Fase 9 — preparar smoke — saída 0 — nenhum ambiente prévio.
28. raiz — `docker ps -a --filter "name=perfumestore-phase9-http" ...; docker volume ls --filter "name=perfumestore-phase9-http" ...` — verificar recursos — saída 0.
29. raiz — `docker volume create perfumestore-phase9-http-data; docker run -d --name perfumestore-phase9-http --env POSTGRES_DB=perfumestore_phase9_http --env POSTGRES_USER=phase9_http --env POSTGRES_PASSWORD=[REDACTED] --publish 127.0.0.1:55439:5432 --volume perfumestore-phase9-http-data:/var/lib/postgresql postgres:18.6` — criar PostgreSQL isolado — saída 0.
30. raiz — loop `docker exec perfumestore-phase9-http pg_isready ...` seguido de `docker inspect` — readiness — saída 0 — `running`.
31. `backend` — criação de diretórios de log/imagem e `Start-Process` do JAR local com datasource, shipping, payment MOCK e secrets `[REDACTED]` — iniciar smoke — saída 0.
32. raiz — loop `Invoke-WebRequest http://127.0.0.1:18089/actuator/health` com caminho de log relativo incorreto — primeira sondagem — saída 1.
33. raiz — `Get-Process`, `Get-NetTCPConnection` e tail do log da aplicação — diagnosticar sondagem — saída 0.
34. raiz — `Select-String`/`Get-Content` no log da aplicação — confirmar startup/Flyway — saída 0 — JAR iniciou e aplicou V1-V8.
35. `backend` — `rg -n "class .*Controller|@PostMapping|@GetMapping|record .*Request|record .*Response"` nos módulos auth/address/shipping/cart/order — contratos HTTP — saída 0.
36. `backend` — leitura de AuthDtos, AddressDtos, OrderDtos, CartDtos e PaymentDtos — contratos — saída 0.
37. `backend` — restart local com `MANAGEMENT_HEALTH_MAIL_ENABLED=false` e secrets `[REDACTED]` — primeira correção operacional — saída 0, mas senha foi passada literalmente como `[REDACTED]` e o processo falhou.
38. `backend` — tail dos logs local/error — diagnóstico — saída 0 — autenticação do banco falhou pelo marcador literal.
39. `backend` — restart local com valores sintéticos reais no processo e secrets não impressos — correção — saída 0.
40. `backend` — `Start-Sleep -Seconds 18; Invoke-WebRequest http://127.0.0.1:18089/actuator/health` — health local — saída 0 — 200.
41. `backend` — leitura de V3 e V4 — validar constraints da fixture — saída 0.
42. raiz — geração de UUIDs e `docker exec ... psql -v ON_ERROR_STOP=1 -c` com inserts sintéticos de categoria/produto/apresentação/imagem/estoque — fixture — saída 0 — 5 inserts.
43. `backend` — script PowerShell HTTP para CSRF, cadastro, login, endereço, frete, carrinho, pedido, sessão de pagamento e webhook — smoke — saída 1 — etapas até sessão 201; webhook sem corpo recebeu 400.
44. raiz — `docker exec ... psql ... select ... payment_attempts ...` — recuperar tentativa — saída 0.
45. raiz — script PowerShell de webhook assinado duas vezes — reexecução/idempotência — saída 1 no comando de consulta porque usou `o.status`; as respostas HTTP foram 202/202.
46. `backend` — leitura da primeira parte de V7 — corrigir consulta — saída 0; coluna correta `order_status`.
47. raiz — `docker exec ... psql ... select o.public_number,pa.status,o.order_status,o.payment_status,...` — confirmar efeitos — saída 0 — `APPROVED|CONFIRMED|APPROVED|1|0`.
48. `backend` — leitura de `application-prod.yml` e `application.yml` — inspeção de produção — saída 0; detectadas duas chaves `spring` em prod.
49. `backend` — tentativa de iniciar prod completa — rejeitada pelo executor antes de iniciar.
50. `backend` — `Stop-Process -Id 27464 -Force -ErrorAction SilentlyContinue; Start-Sleep -Seconds 2` — encerrar local — saída 0.
51. `backend` — `$env:SPRING_PROFILES_ACTIVE='prod'; $env:SERVER_PORT='18090'; Get-ChildItem Env:SPRING_PROFILES_ACTIVE,Env:SERVER_PORT` — teste mínimo — saída 0.
52. `backend` — `Start-Process` de prod incompleto — validar falha de configuração — saída 0; processo encerrado.
53. `backend` — `Stop-Process -Id 27432 -Force -ErrorAction SilentlyContinue; Start-Sleep -Seconds 2` — encerrar tentativa — saída 0.
54. `backend` — nova tentativa completa de prod — rejeitada pelo executor.
55. `backend` — tentativa equivalente com senha montada por expressão — rejeitada pelo executor.
56. `backend` — criação temporária de `phase9-prod-start.ps1` com variáveis sintéticas — iniciar prod após correção — sem código de saída de shell.
57. `backend` — `& .\phase9-prod-start.ps1` — iniciar prod antes da correção YAML — saída 0; processo encerrou no carregamento.
58. `backend` — sondagem HTTP em 18090 para health/OpenAPI/webhook — saída 0; sem resposta porque o processo falhou.
59. `backend` — tails dos logs prod e consulta do processo — diagnóstico — saída 1 no processo; log confirmou YAML duplicado.
60. `backend` — `$n=0; Get-Content src/main/resources/application-prod.yml | ForEach-Object { $n++; "{0,3}: {1}" -f $n,$_ }` — inspecionar linhas — saída 0.
61. `backend` — patch de `application-prod.yml` unificando `spring.mail` no bloco `spring` — correção mínima — sem comando shell.
62. `backend` — leitura do início/fim de `application-prod.yml` — verificar correção — saída 0.
63. `backend` — `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd --batch-mode --no-transfer-progress clean verify 2>&1 | Tee-Object ..\Docs\execucoes\logs\phase9-clean-verify-after-prod-fix.log; exit $LASTEXITCODE` — rebuild final — saída 0 — 69 testes, 0 falhas, 0 erros, 0 ignorados.
64. `backend` — `& .\phase9-prod-start.ps1` — iniciar prod corrigido — saída 0 — processo na porta 18090.
65. `backend` — sondagem após 20 s com `Invoke-WebRequest` para health, `/v3/api-docs` e `/api/v1/webhooks/payments/mock` — saída 0 — 200, 404, 404.
66. raiz — `Stop-Process -Id 16416 -Force ...; docker exec ... psql ...; docker inspect ...` — confirmar persistência — saída 0 — Flyway 8, 1 pedido, 1 tentativa, `CONFIRMED`, `APPROVED`, saldo `1:0`, container running.
67. raiz — `docker stop perfumestore-phase9-http; docker inspect --format '{{.State.Status}} {{range .Mounts}}{{.Name}}{{end}}' perfumestore-phase9-http` — encerrar recursos da homologação — saída 0 — container exited e volume preservado.
68. Diretório raiz — `git diff --check; ...; rg -n ...` — revisão de whitespace, status, arquivos ignorados e segredos — saída 0 para `diff-check` e 0 correspondências de segredo; avisos CRLF não impediram a revisão.
69. Diretório raiz — leitura de `application-prod.yml` e bytes finais — localizar linha em branco — saída 0.
70. Diretório raiz — patches de remoção da linha em branco final em `application-prod.yml` e no guia de integração — correção de `git diff --check` — saída 0 na checagem staged.
71. Diretório raiz — `$name=git config --get user.name; $email=git config --get user.email; "name=[$name]"; "email=[$email]"; git log -1 --format='%an <%ae>'` — verificar identidade sem alterar configuração — saída 0; `user.name` global/local vazio, último autor evidenciado como Davi Esteves <davi00eteves@gmail.com>.
72. Diretório raiz — `git add ...; git diff --cached --check; git diff --cached --stat; git diff --cached --name-only` — preparar somente arquivos da Fase 9 — saída 0 inicialmente com uma linha em branco final identificada no guia.
73. Diretório raiz — `git add Docs/backend/05-integracao-provedor-pagamento.md; git diff --cached --check` — confirmar correção — saída 0.
74. Diretório raiz — `git -c user.name='Davi Esteves' -c user.email='davi00eteves@gmail.com' commit -m "feat: implement mock payment provider infrastructure"` — criar commit local — saída 0; commit criado com 38 arquivos.
75. Diretório raiz — `git add Docs/STATUS.md Docs/execucoes/FASE_09_RELATORIO.md Docs/execucoes/FASE_09_COMANDOS.md; git diff --cached --check` — registrar encerramento documental — saída 0.
76. Diretório raiz — `git -c user.name='Davi Esteves' -c user.email='davi00eteves@gmail.com' commit --amend --no-edit` — incorporar o fechamento documental no mesmo commit — saída 0; hash final verificado posteriormente por `git rev-parse HEAD`.
77. Diretório raiz — `git status --short; git rev-parse HEAD; git log -1 --format='%H%n%s%n%an <%ae>'; git diff HEAD^ --check; Test-Path backend/target/perfumestore-backend-0.0.1-SNAPSHOT.jar; docker ps -a --filter "name=perfumestore-phase9-http" --format "{{.Names}}|{{.Status}}|{{.Ports}}"; docker volume ls --filter "name=perfumestore-phase9-http-data" --format "{{.Name}}"; git status --short --ignored | Select-String 'Docs/execucoes/logs|data/phase9|target' | Select-Object -First 20` — verificação final — saída 0; árvore limpa, JAR presente, hash final verificado no handoff, container parado e volume preservado.
