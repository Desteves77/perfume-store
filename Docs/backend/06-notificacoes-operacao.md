# Operacao de notificacoes transacionais

## Arquitetura

O pedido, o pagamento e o envio gravam uma intencao na outbox PostgreSQL dentro da propria transacao. O `NotificationDispatcher` reivindica registros com lease e `FOR UPDATE SKIP LOCKED`, envia fora da transacao comercial e registra cada tentativa em `notification_attempts`.

## Configuracao

Local/teste usam Mailpit ou capturador de teste. Variaveis principais:

- `NOTIFICATION_ENABLED` habilita a gravacao da outbox.
- `NOTIFICATION_DISPATCH_ENABLED` habilita o scheduler de envio.
- `NOTIFICATION_BATCH_SIZE` define o maximo por ciclo.
- `NOTIFICATION_MAX_ATTEMPTS` define o limite de tentativas.
- `NOTIFICATION_INITIAL_BACKOFF`, `NOTIFICATION_MAX_BACKOFF` e `NOTIFICATION_LEASE_DURATION`.
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` e `MAIL_FROM`.

O perfil de producao nao habilita Mailpit nem MOCK automaticamente. Sem SMTP real e configuracao explicita, a operacao de envio deve permanecer desabilitada.

## Diagnostico e reprocessamento

Use `GET /api/v1/admin/notifications` para verificar estado, tentativas, proximo retry e codigo de falha. O corpo do e-mail nao e retornado. Registros `RETRY_PENDING` sao elegiveis no proximo ciclo; `FAILED` exige procedimento operacional autorizado ou correcao de configuracao antes de nova estrategia de reprocessamento.

Um lease expirado pode ser reivindicado por outro worker. O processamento e idempotente no nivel logico do evento, mas SMTP convencional nao garante exatamente uma entrega se uma falha ocorrer depois da aceitacao pelo servidor e antes do registro local de `SENT`.

## Privacidade

Nao registrar senhas, tokens de recuperacao, cookies, segredos SMTP ou conteudo integral de mensagens em logs. A recuperacao de senha permanece fora da outbox para que o token continue somente como hash no banco.

## Limites

Esta fase nao implementa provedor de e-mail real, templates HTML completos, fila externa, metricas distribuidas ou entrega universal exatamente uma vez. Mailpit e apenas ferramenta local de desenvolvimento/homologacao. A Fase 12 nao foi iniciada.
