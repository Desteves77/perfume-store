# Fase 10 — Testes e verificações

Ainda não executados. Os resultados serão registrados à medida que os cenários forem executados, distinguindo testes focados, regressão, integração PostgreSQL, concorrência, segurança, smoke HTTP, Flyway, persistência e build.
## Testes focados

- Classe: PostSaleIntegrationTest.
- Comando: `.\mvnw.cmd --batch-mode --no-transfer-progress -Dtest=PostSaleIntegrationTest test` com JDK 17.
- Resultado final: 6 testes, 6 aprovados, 0 falhas, 0 erros, 0 ignorados.
- Cobertura: administracao e transicoes logisticas; concorrencia/idempotencia de preparacao; cancelamento e liberacao de reserva; reembolso MOCK sem reposicao; devolucao, recebimento, inspecao e reposicao idempotente; autorizacao e CSRF.

## Smoke HTTP isolado

- Ambiente: Compose `perfumestore-phase10-smoke11`, PostgreSQL 18.6, porta 56411, volume proprio, JAR final em perfil local na porta 18482.
- Resultado: aprovado, `SMOKE_RESULT=PASS`.
- Cenarios: health; autenticacao ADMIN/CUSTOMER; catalogo sintetico; estoque; endereco/frete; carrinho/checkout; aprovacao MOCK; listagem/detalhe; preparar, enviar e entregar; devolucao/recebimento/inspecao/reposicao repetida; solicitacao e aprovacao de reembolso MOCK; cancelamento de pedido pendente; anonimo 401; CUSTOMER 403; escrita sem CSRF 403; reinicio da aplicacao e consulta do pedido.
- Evidencias: pagamento REFUNDED, entrega DELIVERED, uma devolucao, uma solicitacao de reembolso, saldo final fisico 5/reservado 0/disponivel 5.

## Persistencia, migrations e producao

- Apos `docker compose restart postgres`, a aplicacao voltou a health 200.
- Contagens persistidas: 2 pedidos, 4 eventos administrativos, 1 reembolso e 1 devolucao.
- Flyway retornou V1 a V9, com V9 `create admin post sale tables`, sem reaplicacao.
- Perfil prod: health 200; rota de simulador nao acessivel anonimamente (401); `/v3/api-docs` 404; pagamento desabilitado e sem adaptador MOCK carregado.

## Reexecucoes

Houve ensaios HTTP interrompidos por roteiro: produto ativo prematuro (409), ausencia de Idempotency-Key na entrada de estoque (400), porta fixa incorreta, alvo SQL de container anterior e verificacao CSRF em estado incompatível. Cada causa foi corrigida no roteiro ignorado e a execução final em smoke11 passou integralmente.

## Regressao e build

- Comando: `.mvnw.cmd --batch-mode --no-transfer-progress clean verify` com `JAVA_HOME` apontando para Zulu 17.
- Resultado: BUILD SUCCESS; 75 testes, 0 falhas, 0 erros, 0 ignorados.
- Evidência: `backend/target/surefire-reports` e `Docs/execucoes/logs/fase10-clean-verify-1.log`.
- O JAR `backend/target/perfumestore-backend-0.0.1-SNAPSHOT.jar` foi criado. Avisos de encerramento de conexões durante o cleanup do Testcontainers não alteraram o resultado.
## Provisionamento

- Execução dedicada: aprovada, código 0.
- Reexecução idempotente: aprovada, código 0.
- Evidência: `fase10-provision-2.log` e `fase10-provision-2.err.log` na pasta local ignorada; nenhum segredo foi incluído nos documentos.
