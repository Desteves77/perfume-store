# FASE 12 - Testes e verificacoes

## Regressao e build

| Cenario | Comando/ambiente | Resultado |
| --- | --- | --- |
| Baseline anterior | `backend\\mvnw.cmd --batch-mode clean verify`; Testcontainers PostgreSQL 18.6 | 81/81 aprovados; 0 falhas, erros ou ignorados; build/JAR aprovados |
| Isolamento do perfil prod | `backend\\mvnw.cmd --batch-mode -Dtest=ProductionProfileIsolationIntegrationTest test` | 1/1 aprovado; sem MOCK e com `UnavailablePaymentProvider` |
| Reexecucao final completa | `backend\\mvnw.cmd --batch-mode clean verify` | 82/82 aprovados; 0 falhas, 0 erros, 0 ignorados; BUILD SUCCESS |

Os logs de desligamento dos containers de teste exibem warnings de schedulers JDBC/Hikari quando o banco do teste ja foi encerrado. A suite terminou aprovada; esses registros sao ruido de shutdown do ambiente de teste, nao falhas de assercao.

## Integracao HTTP/HTTPS

- Compose isolado: PostgreSQL 18.6, app Java 17, Mailpit e Caddy.
- Health, liveness, readiness, CSRF e OpenAPI local responderam 200 sobre HTTPS.
- Jornada cliente: cadastro, login, catalogo, imagem, entrada de estoque, carrinho anonimo, merge, endereco, quote, checkout, reserva, pagamento MOCK, notificacao, consulta e persistencia.
- Jornada administrativa: listagem, prepare, ship, tracking, deliver, cancelamento idempotente, reembolso MOCK, devolucao, recebimento, inspecao e reposicao idempotente.
- Seguranca: ADMIN 200; CUSTOMER em rota ADMIN 403; anonimo 401; mutacao sem CSRF 403 `CSRF_INVALID`.
- Mailpit: tres mensagens transacionais capturadas no fluxo normal: pedido recebido, pagamento aprovado e pedido enviado.
- HTTPS: HTTP redirecionou para HTTPS; cookies de sessao/CSRF/cart foram emitidos com `Secure` e `SameSite=Lax` no ambiente de homologacao.

## Concorrencia, idempotencia e isolamento

A suite existente cobre PostgreSQL real, locks, reservas, checkout, pagamentos, webhooks, reconciliacao, pos-venda e outbox. O smoke comprovou repeticao de cancelamento, reembolso e reposicao sem duplicacao de efeito. O teste de perfil prod comprova ausencia de beans MOCK; com sessao autorizada a rota MOCK respondeu 404, e pagamento sem provedor respondeu 503.

## Flyway e persistencia

- V1-V10 foram aplicadas em bancos limpos dos testes e a ultima versao observada foi V10.
- Nenhum arquivo de migration foi alterado.
- Restart de PostgreSQL/app preservou pedidos, estados e historico.
- Backup `pg_dump -Fc` foi restaurado em banco novo; a aplicacao iniciou sobre ele e consultou dados.
- O volume de imagens foi exportado e o tar foi listado com sucesso.

## Backup/restore

- Dump: `phase12-backups/phase12-proof.dump`, SHA-256 `18577CA110756A0B3ACF754E691EFCB92792A0B65F6349A8D71470401EED124E`.
- Imagens: `phase12-backups/phase12-images.tgz`, SHA-256 `AA4B687D51C13780A6A22F517AA3CE7C32DB07A2266B2B0372A0C9F228F684DF`.
- Banco restaurado: `perfumestore_phase12_restore_proof`.
- Verificacao restaurada: Flyway v10; 2 usuarios, 2 pedidos, 1 saldo de inventario, 4 notificacoes; app sobre o banco restaurado respondeu readiness 200.

## Evidencias nao executadas como producao

Nao foram executados provedor financeiro real, SMTP real, deploy publico ou transacoes financeiras reais. Esses itens permanecem pendentes e nao sao marcados como aprovados.
