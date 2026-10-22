# Fase 5 — Testes e evidências

## Matriz planejada e resultado

| Cenário | Tipo | Evidência | Estado |
|---|---|---|---|
| Migration V4 incremental e saldo zero inicial | integração PostgreSQL | `InventoryIntegrationTest` | aprovado |
| Entrada/ajuste válido e histórico | HTTP/integração | teste focado e smoke HTTP | aprovado |
| Quantidade inválida e chave idempotente | HTTP/integração | `InventoryIntegrationTest` e smoke | aprovado |
| Reserva acima do disponível | integração transacional | `InventoryIntegrationTest` | aprovado |
| Duas reservas da última unidade | concorrência PostgreSQL | `InventoryIntegrationTest` | aprovado |
| Confirmação/liberação repetidas e incompatíveis | integração transacional | `InventoryIntegrationTest` | aprovado |
| Confirmação versus liberação concorrentes | concorrência PostgreSQL | `InventoryIntegrationTest` | aprovado |
| CUSTOMER/anônimo/CSRF nos endpoints ADMIN | MockMvc/Security e HTTP | teste focado e smoke | aprovado |
| Disponibilidade pública sem dados administrativos | MockMvc/catalog e HTTP | teste focado e smoke | aprovado |
| Regressão da suíte existente e build | `clean verify` | Surefire/JAR | aprovado |
| Smoke HTTP e persistência após restart | ambiente local isolado | logs sanitizados e consulta PostgreSQL | aprovado |

## Registros

Os comandos e resultados foram registrados durante a execução. Nenhum token, cookie, senha ou credencial foi registrado.

## Resultado dos testes focados

Comando: `backend\.mvnw.cmd --batch-mode -Dtest=InventoryIntegrationTest test`, com JDK 17.0.16, Testcontainers 2.0.5 e PostgreSQL 18.6. Resultado: **8 testes executados, 8 aprovados, 0 falhas, 0 erros, 0 ignorados**. O relatório está em `backend/target/surefire-reports/com.perfumestore.backend.InventoryIntegrationTest.txt` e o log sanitizado em `Docs/execucoes/logs/phase5-inventory-test-01.log`.

Os testes cobriram saldo zero sob demanda e independência por apresentação, entrada administrativa HTTP com idempotência, CUSTOMER e CSRF, quantidade inválida, chave reutilizada, reserva acima do saldo, apresentação inativa, duas reservas concorrentes da última unidade, confirmação/liberação idempotentes, transições terminais concorrentes e disponibilidade pública sem exposição de físico/reservado.

## Resultado final

- `InventoryIntegrationTest`: 8 testes executados, 8 aprovados, 0 falhas, 0 erros e 0 ignorados.
- `clean verify`: 41 testes executados, 41 aprovados, 0 falhas, 0 erros e 0 ignorados.
- Classes executadas: `AdminProvisioningIntegrationTest` (2), `ApiContractIntegrationTest` (12), `AuthenticationIntegrationTest` (10), `CatalogIntegrationTest` (6), `InfrastructureIntegrationTest` (2), `InventoryIntegrationTest` (8) e `SessionPersistenceIntegrationTest` (1).
- JAR executável gerado em `backend/target/perfumestore-backend-0.0.1-SNAPSHOT.jar`.
- Relatórios Surefire em `backend/target/surefire-reports/`.

## Concorrência, segurança e smoke

Os cenários concorrentes usaram PostgreSQL real e transações independentes para duas reservas da última unidade, confirmações/liberações concorrentes e transições terminais. O estado final e o número de movimentações foram verificados no banco.

O smoke HTTP isolado passou em 16 cenários: health, login ADMIN, perfil, saldo inicial, entrada, reenvio idempotente, histórico, OpenAPI, escrita sem CSRF, acesso anônimo, cadastro CUSTOMER, acesso CUSTOMER, catálogo público e ausência de saldos internos. O resultado sanitizado está em `Docs/execucoes/logs/phase5-smoke-results.json`, ignorado pelo Git.

Antes do restart, o banco registrava físico 5, reservado 0, disponível 5 e uma movimentação para a apresentação sintética. Depois do restart, a aplicação respondeu health 200, o endpoint ADMIN retornou os mesmos saldos, o catálogo retornou disponibilidade 5 e o Flyway indicou V1–V4 válidas, com schema na versão 4 e sem reaplicação.

Após a correção da semântica de `ADJUSTMENT`, o foco reexecutado (`phase5-inventory-test-03.log`) permaneceu em 8/8/0/0/0 e o `clean verify` final (`phase5-clean-verify-02.log`) permaneceu em 41/41/0/0/0. O smoke HTTP foi repetido com o JAR final e passou novamente em 16/16 cenários.

Após a consolidação da leitura de disponibilidade da listagem pública, `clean verify` foi executado novamente (`phase5-clean-verify-03.log`) com 41/41/0/0/0 e o smoke HTTP final passou em 16/16 cenários. A alteração não mudou o contrato público; apenas removeu consultas de saldo repetidas por produto.
