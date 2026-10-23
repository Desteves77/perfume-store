# Fase 11 — Testes e verificações

## Inspeção pré-implementação

- A suíte existente usa Testcontainers com PostgreSQL 18.6.
- O teste de recuperação usa `CapturedEmailSender` para obter o link somente pelo capturador de teste.
- Os testes existentes de pedidos, pagamentos e administração já cobrem transações e estados comerciais; a Fase 11 adicionará outbox, deduplicação, retry, lease e regressão das notificações.

Os testes serão registrados durante a execução, distinguindo testes focados, integração PostgreSQL, Mailpit, concorrência, falhas, regressão, smoke HTTP e build.

## Testes executados

| Grupo | Comando/ambiente | Resultado |
|---|---|---|
| Focado inicial | `backend\\mvnw.cmd -q -Dtest=NotificationIntegrationTest test` | Uma primeira execução falhou por fixture FK; após corrigir os IDs de pedido, 5/5 passaram. |
| Focado final | `backend\\mvnw.cmd -q -Dtest=NotificationIntegrationTest test` | 6 testes, 0 falhas, 0 erros, 0 ignorados. PostgreSQL 18.6 via Testcontainers. |
| Regressão focada | `Auth, Order, Payment, PostSale` com Maven Surefire | 27 testes, 0 falhas, 0 erros, 0 ignorados. |
| Regressão final | `backend\\mvnw.cmd -q clean verify` | 81 testes, 0 falhas, 0 erros, 0 ignorados; JAR gerado. A primeira tentativa ficou presa em timeout do Docker/Testcontainers e não foi usada como resultado. |
| Outbox | `NotificationIntegrationTest` | Deduplicação por evento, rollback sem outbox, retry, estado e claim concorrente. |
| Templates | `NotificationIntegrationTest` | Texto, HTML alternativo, escape de nome/item, assunto, BRL e ausência de segredo. |
| Segurança | `NotificationIntegrationTest` e smoke | Endpoint ADMIN anônimo rejeitado; consulta autenticada ADMIN funcionou; mutações comerciais continuaram sob CSRF. |
| SMTP/Mailpit | JAR final + Mailpit isolado | Pedido recebido, pagamento aprovado e pedido enviado capturados; `notificationStatuses` final somente `SENT`. |
| Falha SMTP | JAR/Mailpit isolados | Primeira execução demonstrou `RETRY_PENDING`; após a correção multipart, reprocessamento entregou as mensagens. |
| Persistência | PostgreSQL dedicado restart + JAR | V10 validada sem reaplicação; pedidos, notificações e saldo permaneceram. |

## Smoke final

- Health: `200`.
- Pedido recebido: criado em `PENDING_PAYMENT`.
- Pagamento simulado: `APPROVED`; notificação de pagamento entregue.
- Envio administrativo: `PREPARING` -> `SHIPPED`; notificação de envio entregue.
- Mailpit: assuntos `Pedido recebido`, `Pagamento aprovado` e `Pedido enviado` observados para o pedido da execução final.
- Consulta ADMIN: notificações paginadas retornaram tipos e estados; nenhum corpo HTML/texto foi exposto.
- Persistência: Flyway V10, 15 notificações e 15 `SENT` após reinicialização do PostgreSQL isolado.

## Limitações registradas

- SMTP convencional não fornece garantia universal de exatamente uma entrega quando o servidor aceita a mensagem e a aplicação cai antes de marcar `SENT`; a implementação oferece deduplicação lógica, message-id estável, lease, tentativas e diagnóstico.
- A recuperação de senha foi mantida fora da outbox para não persistir token recuperável; seu fluxo existente continuou coberto pela regressão.
- Não há provedor real, credencial real, envio externo ou operação comercial de produção nesta fase.
