# Fase 8 — Registro de testes

## Baseline

A referência histórica da Fase 7 é de 58 testes aprovados. Ela não é tratada como resultado atual até a suíte ser executada novamente.

## Execução

## Resultados finais

| Grupo | Ambiente/comando | Resultado |
|---|---|---|
| Compilação | `JAVA_HOME=JDK17 .\mvnw.cmd -q -DskipTests compile` | Primeira tentativa falhou por construtor incompleto; após correção passou. |
| Foco | `-Dtest=OrderIntegrationTest,CatalogIntegrationTest` | 10/10 aprovados após atualização da expectativa de V7. |
| Pedidos | `-Dtest=OrderIntegrationTest` | 6/6 aprovados após correção da corrida de idempotência. |
| Regressão | `mvnw test` | 64 testes, 0 falhas, 0 erros, 0 ignorados. |
| Build | `mvnw clean verify` com JDK17 | BUILD SUCCESS, 64 testes, JAR executável. A mensagem do scheduler de sessões no encerramento do fork foi não bloqueante. |
| Flyway | PostgreSQL 18.6 isolado | V1–V7 aplicadas/validadas; restart informou schema atualizado. |
| Concorrência | Testcontainers PostgreSQL real | Mesma chave criou um pedido; dois clientes não reservaram a última unidade duas vezes; rollback integral funcionou. |
| Segurança | MockMvc e HTTP | CUSTOMER/anônimo protegidos; 401 anônimo, 403 sem CSRF e simulador ausente fora de `local`/`test`. |
| Smoke | JAR final + PostgreSQL/Mailpit isolados | Health 200, registro/login/claim, endereço/cotação, pedido 70,90 BRL, replay, aprovação simulada, estoque final físico 2/reservado 0. |

## Smoke negativo e persistência

- Checkout anônimo com CSRF válido: HTTP 401.
- Mutação autenticada sem header CSRF: HTTP 403.
- Reuso da chave com payload diferente: HTTP 409, rejeição esperada.
- Após restart do JAR mantendo o volume: health HTTP 200, um pedido recuperado e carrinho do cliente preservado.
- OpenAPI no JAR final: `/v3/api-docs` respondeu com 38.429 bytes e publicou `/api/v1/checkout/orders`, `/api/v1/me/orders` e `/api/v1/dev/payment/orders/{publicNumber}`.

Nenhum cartão, credencial de produção, token, cookie ou segredo foi armazenado nos registros versionados. Saídas longas permanecem em `Docs/execucoes/logs/`, ignorado pelo Git.

Os cenários, comandos, contagens, falhas, correções, reexecuções, testes de PostgreSQL/concurrency, smoke HTTP, Flyway, persistência e build serão registrados durante a implementação. Nenhum teste não executado será marcado como aprovado.
