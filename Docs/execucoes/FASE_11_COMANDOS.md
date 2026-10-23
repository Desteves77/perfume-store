# Fase 11 — Comandos executados

Os comandos serão registrados em ordem cronológica, com diretório, finalidade, código de saída e resultado sanitizado. Segredos serão substituídos por `[REDACTED]`.

## Limitações de captura

## Comandos registrados até aqui

1. Diretório: `C:\Users\User\Desktop\Ecommerce PerfumeStore`
   Comando: `Get-Content backend/src/main/resources/db/migration/V7__create_order_tables.sql; Get-Content backend/src/main/resources/db/migration/V8__create_payment_tables.sql; Get-Content backend/src/main/resources/db/migration/V9__create_admin_operations_tables.sql`
   Finalidade: inspecionar migrations de pedidos, pagamentos e operação administrativa. Resultado: saída de V7 e V8; V9 não foi localizada com o nome inicialmente informado. Código: `1`.

2. Diretório: `C:\Users\User\Desktop\Ecommerce PerfumeStore`
   Comando: `Get-ChildItem backend/src/main/resources/db/migration | Select-Object -ExpandProperty Name; Get-Content backend/src/main/resources/db/migration/V9__admin_operations.sql; rg -n "interface UserRepository|class UserRepository|record User|enum Order|applyPaymentApproved|class AdminOrderService|void sendPasswordRecovery|CapturedEmail|@Scheduled|reconcile" backend/src/main/java backend/src/test/java`
   Finalidade: confirmar numeração real e localizar componentes de identidade, e-mail, pedidos, pagamentos e schedulers. Resultado: confirmou V1–V9 e identificou o nome real da V9; a leitura do nome alternativo falhou; a busca retornou os componentes relevantes. Código: `1`.

3. Diretório: `C:\Users\User\Desktop\Ecommerce PerfumeStore`
   Comando: `Get-Content backend/src/main/resources/db/migration/V9__create_admin_post_sale_tables.sql; Get-Content backend/src/main/java/com/perfumestore/backend/user/UserRepository.java; Get-Content backend/src/main/java/com/perfumestore/backend/notification/EmailSender.java; Get-Content backend/src/main/java/com/perfumestore/backend/notification/CapturedEmailSender.java; Get-Content backend/src/main/java/com/perfumestore/backend/notification/SmtpEmailSender.java`
   Finalidade: ler o schema V9 e a infraestrutura existente de usuários e e-mail. Resultado: V9 contém logística, histórico administrativo, reembolsos e devoluções; o transporte atual aceita somente recuperação de senha. Código: `0`.

4. Diretório: `C:\Users\User\Desktop\Ecommerce PerfumeStore`
   Comando: `Get-Content backend/src/test/java/com/perfumestore/backend/AuthenticationIntegrationTest.java; Get-Content backend/src/test/java/com/perfumestore/backend/SessionPersistenceIntegrationTest.java; Get-Content backend/src/main/java/com/perfumestore/backend/order/OrderService.java; Get-Content backend/src/main/java/com/perfumestore/backend/order/AdminOrderService.java`
   Finalidade: revisar recuperação de senha, sessão, criação/aprovação de pedidos e envio administrativo. Código: `0`.

5. Diretório: `C:\Users\User\Desktop\Ecommerce PerfumeStore`
   Comando: `Get-Content backend/src/main/java/com/perfumestore/backend/order/OrderService.java | Select-Object -First 230; Get-Content backend/src/main/java/com/perfumestore/backend/order/OrderRepository.java | Select-Object -First 320; Get-Content backend/src/main/java/com/perfumestore/backend/payment/PaymentService.java | Select-Object -First 250; Get-Content backend/src/main/java/com/perfumestore/backend/config/OrderProperties.java; Get-Content backend/src/main/java/com/perfumestore/backend/config/PaymentProperties.java`
   Finalidade: confirmar limites transacionais e integração de pagamento. Código: `0`.

6. Diretório: `C:\Users\User\Desktop\Ecommerce PerfumeStore`
   Comando: `Get-Content backend/src/main/java/com/perfumestore/backend/PerfumeStoreApplication.java; Get-Content backend/src/main/resources/application.yml; Get-Content backend/src/main/resources/application-local.yml; Get-Content backend/src/main/resources/application-test.yml; Get-Content backend/src/main/resources/application-prod.yml; Get-Content backend/src/main/java/com/perfumestore/backend/order/OrderExpirationScheduler.java; Get-Content backend/src/main/java/com/perfumestore/backend/payment/PaymentReconciliationScheduler.java; rg -n "Mailpit|smtp|password recovery|e-mail|notifica|outbox|@Scheduled|notification" README.md Docs/backend Docs/STATUS.md Docs/ROADMAP_BACKEND.md`
   Finalidade: revisar configuração, schedulers e documentação. Código: `1`; o caminho do scheduler de reconciliação não existe.

7. Diretório: `C:\Users\User\Desktop\Ecommerce PerfumeStore`
   Comando: `Get-Content backend/src/main/java/com/perfumestore/backend/order/OrderRecord.java; Get-Content backend/src/main/java/com/perfumestore/backend/order/OrderItemRecord.java; Get-Content backend/src/main/java/com/perfumestore/backend/order/OrderShippingRecord.java; Get-Content backend/src/main/java/com/perfumestore/backend/user/AppUser.java; Get-Content backend/src/main/java/com/perfumestore/backend/order/OrderDtos.java | Select-Object -First 260; Get-Content backend/src/main/java/com/perfumestore/backend/order/AdminOrderDtos.java | Select-Object -First 220`
   Finalidade: conferir registros e DTOs necessários aos templates. Código: `0`; itens e frete do pedido são package-private.

Operações de edição aplicadas por `apply_patch` não possuem linha shell reproduzível; serão validadas pelo diff e pelos testes.

Edições feitas por `apply_patch` e consultas de acompanhamento de processos não possuem uma linha shell reproduzível; essa limitação será registrada sem inventar comandos.

## Comandos posteriores registrados

8. Diretório `C:\Users\User\Desktop\Ecommerce PerfumeStore`. Comando: `docker compose -p perfumestore-phase11-acceptance -f compose.phase11-acceptance.yaml --env-file phase11-acceptance.env.example config --quiet`. Finalidade: validar Compose isolado. Código `0`; configuração válida.

9. Mesmo diretório. Comando: `docker compose -p perfumestore-phase11-acceptance -f compose.phase11-acceptance.yaml --env-file phase11-acceptance.env.example up -d postgres mailpit`. Finalidade: iniciar somente PostgreSQL/Mailpit isolados. Código `0`; ambos ficaram saudáveis.

10. Mesmo diretório. Comando: `docker exec perfumestore-phase11-acceptance-postgres-1 psql -U [REDACTED] -d [REDACTED] -c "insert ..."`. Finalidade: primeira fixture SQL. Código `0` do pipeline, mas as relações ainda não existiam; tentativa inválida registrada e não usada.

11. Mesmo diretório. Comando: `docker exec perfumestore-phase11-acceptance-postgres-1 psql -v ON_ERROR_STOP=1 -U [REDACTED] -d [REDACTED] -c "insert ..."`. Finalidade: fixture sintética após migrations. Código `0`; categoria, produto, apresentação, imagem e saldo foram criados.

12. Mesmo diretório. Comando: `& '.\\Docs\\execucoes\\logs\\start-phase11-smoke-app.ps1'; ... health ...`. Finalidade: inicialização do JAR com Java17, porta 18580 e datasource isolado. Código `0`; health `200`, Flyway V10.

13. Mesmo diretório. Comando: `& '.\\Docs\\execucoes\\logs\\phase11-http-mailpit-smoke.ps1' *> '.\\Docs\\execucoes\\logs\\fase11-http-mailpit-smoke-final.log'`. Finalidade: smoke HTTP/Mailpit. Código de processo `0`, mas resultado funcional parcial: mensagens ficaram `RETRY_PENDING` e envio não avançou com chave fixa; defeito investigado.

14. Mesmo diretório. Comando: `Get-Content ... fase11-smoke-app.log; Get-Content ... fase11-smoke-app.err.log`. Finalidade: diagnosticar envio. Código `0`; log revelou erro de `MimeMessageHelper` sem multipart.

15. Mesmo diretório. Comando: `& '.\\Docs\\execucoes\\logs\\phase11-http-mailpit-smoke.ps1' *> '.\\Docs\\execucoes\\logs\\fase11-http-mailpit-smoke-final-02.log'`. Finalidade: repetir smoke com chaves de preparação/envio únicas. Código `0`; pedido `SHIPPED`, Mailpit recebeu três tipos e estados ficaram `SENT`.

16. Diretório `backend`. Comando: `& '.\\mvnw.cmd' -q -Dtest=NotificationIntegrationTest test *> '..\\Docs\\execucoes\\logs\\fase11-notification-final-test.log'`. Finalidade: validar notificações após correções. Código `0`; 6/6.

17. Diretório `backend`. Comando: `& '.\\mvnw.cmd' -q clean verify *> '..\\Docs\\execucoes\\logs\\fase11-clean-verify-final.log'`. Finalidade: regressão/build final inicial. A execução ficou presa no Testcontainers; os processos foram encerrados após timeout de readiness, sem usar esse resultado como aprovação.

18. Mesmo diretório. Comando: `Stop-Process -Id [processos Maven/Testcontainers desta execução] -Force`. Finalidade: encerrar somente processos da tentativa travada. Código `0`; processos encerrados.

19. Mesmo diretório. Comando: `docker stop admiring_leavitt; docker rm admiring_leavitt`. Finalidade: liberar container PostgreSQL temporário da tentativa. Código `1`; o stop removeu o container via reaper antes do `rm`, portanto o segundo comando informou inexistência.

20. Diretório `backend`. Comando: `& '.\\mvnw.cmd' -q clean verify *> '..\\Docs\\execucoes\\logs\\fase11-clean-verify-retry.log'`. Finalidade: repetir regressão/build após estabilização do Docker. Código `0`; relatórios Surefire: 81/81, sem falhas/erros/ignorados; JAR gerado.

21. Mesmo diretório. Comando: `& '.\\Docs\\execucoes\\logs\\start-phase11-smoke-app.ps1'; ... health ...`. Finalidade: subir JAR final para smoke. Código `0`; health `200`.

22. Mesmo diretório. Comando: `& '.\\Docs\\execucoes\\logs\\phase11-http-mailpit-smoke.ps1' *> '.\\Docs\\execucoes\\logs\\fase11-http-mailpit-smoke-final-jar.log'`. Finalidade: smoke final contra JAR, primeira tentativa. Código `1`; fixture sem saldo retornou `409` ao adicionar item.

23. Mesmo diretório. Comando: `Get-Content ... fase11-http-mailpit-smoke-final-jar.log; Get-Content ... fase11-smoke-app.err.log`. Finalidade: confirmar a causa do `409`. Código `0`; estoque sintético esgotado.

24. Mesmo diretório. Comando: `Invoke-RestMethod POST /api/v1/admin/inventory/adjustments` com sessão ADMIN, CSRF e chave idempotente sintética. Finalidade: repor somente a fixture isolada pelo endpoint real. Código `0`; saldo físico 5, reservado 0, disponível 5.

25. Mesmo diretório. Comando: `& '.\\Docs\\execucoes\\logs\\phase11-http-mailpit-smoke.ps1' *> '.\\Docs\\execucoes\\logs\\fase11-http-mailpit-smoke-final-jar-02.log'`. Finalidade: repetir smoke final. Código `0`; pedido `PS-81268E923F8947A2A21CF6C9D9EA8409`, pagamento aprovado, envio `SHIPPED`, assuntos dos três e-mails capturados, notificações `SENT`.

26. Mesmo diretório. Comando: `Stop-Process -Name java ...`. Finalidade: encerrar JAR antes do restart do banco. Código `0`.

27. Mesmo diretório. Comando: `docker compose -p perfumestore-phase11-acceptance -f compose.phase11-acceptance.yaml --env-file phase11-acceptance.env.example restart postgres; docker compose ... ps; docker exec ... psql ...`. Finalidade: validar persistência do PostgreSQL dedicado. Código `1` na primeira consulta porque uma tabela/coluna foi nomeada incorretamente; o container ficou saudável.

28. Mesmo diretório. Comando: `docker exec ... psql ... "\\dt perfumestore.*"`; depois consulta a `flyway_schema_history` e `inventory_balances`. Finalidade: corrigir a consulta sem alterar dados. Código `0`; V10 aplicada, 6 pedidos, 15 notificações, 15 `SENT`, saldo físico 4/reservado 0/disponível 4.

29. Mesmo diretório. Comando: `& '.\\Docs\\execucoes\\logs\\start-phase11-smoke-app.ps1'; ... Invoke-RestMethod /actuator/health`. Finalidade: reiniciar JAR após restart do banco. Código `0`; health `UP`, Flyway validou 11 migrations e não reaplicou V10.

30. Mesmo diretório. Comando: `rg -n "@Profile|MOCK|MockPayment|dev/payment|NotificationAdminController" backend/src/main/java/...; git diff --stat; git diff --check; rg -n "password|secret|..." ...`. Finalidade: revisar isolamento MOCK, diff e segredos. Código `0` para as verificações; warnings apenas de conversão CRLF do Git e placeholders/valores sintéticos de teste.

31. Mesmo diretório. Comando: `& '.\\Docs\\execucoes\\logs\\phase11-http-mailpit-smoke.ps1'` após reinício do JAR, com chaves únicas. Finalidade: smoke final contra o JAR final após reposição da fixture isolada. Código `0`; health 200, pedido criado, aprovação, envio e três assuntos de e-mail capturados.

32. Mesmo diretório. Comando: `Stop-Process -Name java ...`. Finalidade: encerrar o JAR antes da verificação de persistência. Código `0`.

33. Mesmo diretório. Comando: `docker compose -p perfumestore-phase11-acceptance -f compose.phase11-acceptance.yaml --env-file phase11-acceptance.env.example restart postgres; docker compose ... ps; docker exec ... psql ...`. Finalidade: reiniciar banco dedicado e consultar Flyway, pedidos, notificações e saldo. Código `1` na consulta inicial por nome incorreto de tabela/coluna; nenhuma alteração de dados.

34. Mesmo diretório. Comando: `docker exec ... psql ... "\\dt perfumestore.*"` e consulta corrigida a `flyway_schema_history`, `notifications` e `inventory_balances`. Finalidade: validar persistência com nomes reais. Código `0`; V10, 6 pedidos, 15 notificações/15 `SENT`, físico 4/reservado 0/disponível 4.

35. Mesmo diretório. Comando: `& '.\\Docs\\execucoes\\logs\\start-phase11-smoke-app.ps1'; ... Invoke-RestMethod /actuator/health`. Finalidade: reiniciar aplicação após restart do banco. Código `0`; health `UP`, Flyway validou 11 migrations sem reaplicar.

36. Mesmo diretório. Comando: `Invoke-RestMethod http://127.0.0.1:18580/v3/api-docs; rg -n '@Profile|class UnavailablePaymentProvider' backend/src/main/java/com/perfumestore/backend/payment`. Finalidade: conferir OpenAPI e isolamento produtivo do MOCK. Código `0` na segunda execução; OpenAPI `200` incluiu `/api/v1/admin/notifications`; controllers MOCK estão em local/test e o provedor indisponível fora desses perfis. A primeira busca PowerShell com `&` não escapado retornou código `1` e foi repetida corretamente.

37. Mesmo diretório. Comando: `Stop-Process -Name java ...; docker compose -p perfumestore-phase11-acceptance -f compose.phase11-acceptance.yaml --env-file phase11-acceptance.env.example stop postgres mailpit; docker compose ... ps -a; docker volume ls ...`. Finalidade: encerrar somente recursos da homologação e confirmar preservação. Código `0`; containers parados e volume `perfumestore-phase11-acceptance_perfumestore-phase11-notification-postgres-data` preservado.

38. Mesmo diretório. Comando: `git diff --check; git status --short; git diff --name-only; git ls-files backend/src/main/resources/db/migration`. Finalidade: revisão de whitespace, escopo, migrations anteriores e arquivos. Código `0`; V1-V9 rastreadas e preservadas; V10 nova e alterações somente no escopo da fase.

Operações de edição e exclusão dos scripts temporários foram realizadas por `apply_patch`; não possuem uma linha shell reproduzível. O status e o diff serão conferidos novamente após o commit local.

39. Diretório `C:\Users\User\Desktop\Ecommerce PerfumeStore`. Comando: `git add [arquivos da Fase 11]; git diff --cached --check; git diff --cached --stat; git diff --cached --name-status`. Finalidade: preparar e revisar o commit. Código `0` após remover trailing whitespace; 37 arquivos staged, V1-V9 preservadas.

40. Mesmo diretório. Comando: `git commit -m "feat: implement reliable transactional email notifications"`. Finalidade: criar commit local único da fase. Código `0`; commit inicial criado como `c537ab7`.

41. Mesmo diretório. Comando: `git rev-parse HEAD; git status --short; git remote -v; git show --stat --oneline --decorate --no-renames HEAD; docker compose -p perfumestore-phase11-acceptance -f compose.phase11-acceptance.yaml --env-file phase11-acceptance.env.example ps -a`. Finalidade: verificação pós-commit. Código `0`; HEAD completo `c537ab7b337db735f486f4f6d13a14b574a0976a`, árvore limpa, sem remotes, containers parados.

42. Diretório `C:\Users\User\Desktop\Ecommerce PerfumeStore`. Comando: `git add Docs/execucoes/FASE_11_RELATORIO.md Docs/execucoes/FASE_11_COMANDOS.md; git diff --cached --check; git diff --cached --stat; git commit --amend --no-edit`. Finalidade: incluir hash/encerramento nos registros mantendo um único commit final. Código `0`; o hash foi atualizado.

43. Mesmo diretório. Comando: `git rev-parse HEAD; git status --short; git remote -v; git diff --check; docker compose -p perfumestore-phase11-acceptance -f compose.phase11-acceptance.yaml --env-file phase11-acceptance.env.example ps -a`. Finalidade: verificação final após amend. Código `0`; árvore limpa, sem remotes, diff limpo, containers parados e sem push/deploy.
