# Fase 9 — testes e verificações

## Linha de base

- Referência histórica: 64 testes na Fase 8.
- A contagem desta execução somente será registrada após a execução real da suíte.

## Cenários planejados

- Criação e repetição de sessão de checkout MOCK.
- Assinatura HMAC válida, ausente, inválida e corpo adulterado.
- Aprovação, recusa, cancelamento e expiração.
- Eventos duplicados e fora de ordem.
- Divergência de valor e moeda.
- Confirmação idempotente de reservas.
- Aprovação tardia em `PAYMENT_REVIEW`.
- Reconcilição de pagamento pendente.
- Concorrência com PostgreSQL real.
- Ausência do MOCK no perfil `prod`.
- Persistência após reinicialização e validação Flyway V8.

Os resultados serão preenchidos somente com evidências de comandos realmente executados.
## Resultados executados

| Grupo | Comando/ambiente | Resultado |
|---|---|---|
| Pagamento focado | `-Dtest=PaymentIntegrationTest test` | 5 testes, 5 aprovados, 0 falhas, 0 erros, 0 ignorados. |
| Regressão de pedidos | `-Dtest=OrderIntegrationTest test` | 6 testes, 6 aprovados, 0 falhas, 0 erros, 0 ignorados. |
| Suíte final | `clean verify`, JDK 17, Testcontainers/PostgreSQL 18.6 | 69 testes, 69 aprovados, 0 falhas, 0 erros, 0 ignorados; JAR gerado. |
| Flyway | bancos Testcontainers e PostgreSQL isolado | V1–V8 aplicadas; V8 incremental e V1–V7 preservadas. |
| Concorrência | `PaymentIntegrationTest` com transações independentes | Evento aprovado duplicado processado uma vez; reserva e baixa não duplicadas. |
| Segurança | testes existentes + HMAC | Assinatura inválida e divergência de valor encaminhadas a `REVIEW`; sem efeito de estoque. |
| Aprovação tardia | `PaymentIntegrationTest` | Pedido expirado entra em `PAYMENT_REVIEW` sem recriar reserva nem baixar estoque. |
| Reconciliação | `PaymentIntegrationTest` | Aprovação descoberta no MOCK aplicada; segunda execução idempotente. |

## Smoke HTTP isolado

- PostgreSQL `postgres:18.6` em `127.0.0.1:55439`, container `perfumestore-phase9-http`, volume `perfumestore-phase9-http-data`.
- JAR em `127.0.0.1:18089`, perfil `local`, dados sintéticos e segredo HMAC fora dos registros.
- Aprovados: health 200; cadastro 201; login 200; perfil 200; endereço 201; cotação 200; carrinho 200; checkout 201; sessão MOCK 201/PENDING; webhook assinado 202; reenvio 202; pagamento `APPROVED`; pedido `CONFIRMED`.
- Banco confirmou uma unidade baixada, reserva zero e ausência de efeito duplicado.
- A primeira chamada do webhook no script recebeu 400 por omissão do corpo; a reexecução correta passou e é a evidência válida.

## Perfil de produção

- Após corrigir a estrutura YAML de `application-prod.yml`, o JAR iniciou com `prod` e health 200.
- `/v3/api-docs` respondeu 404.
- `/api/v1/webhooks/payments/mock` respondeu 404.
- `PAYMENT_ENABLED=false`; nenhuma aprovação MOCK foi possível no perfil.

## Persistência e logs

- A consulta posterior confirmou Flyway v8, pedido `CONFIRMED`, tentativa `APPROVED` e saldo físico/reservado `1/0`.
- O container foi parado sem remoção do volume. Logs brutos permanecem em `Docs/execucoes/logs/`, ignorado pelo Git.
- Avisos de conexão nos testes ocorreram no encerramento de containers Testcontainers entre contextos; não geraram falhas ou erros.
