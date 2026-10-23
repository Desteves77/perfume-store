# Fase 11 — Relatório de execução

## Estado inicial

- Diretório: `C:\Users\User\Desktop\Ecommerce PerfumeStore`.
- Branch: `main`.
- HEAD inicial: `9774ab65bc75a7f062051ab7d24d12d4631a4362`.
- Árvore inicial limpa.
- Nenhum `origin` configurado.
- Java direto no PATH é 8; Maven Wrapper utiliza Zulu JDK 17.0.16.
- Migrations aplicadas no código: V1–V9.
- Não existe `AGENTS.md` adicional.

## Objetivo e plano

Implementar notificações transacionais persistentes, desacopladas do fluxo comercial, com Mailpit local, retentativas, idempotência, histórico e configuração segura para futura substituição por serviço real.

Plano: inspecionar contratos e eventos existentes; escolher uma implementação mínima compatível; criar migration incremental; integrar pedido recebido, pagamento aprovado, pedido enviado e recuperação de senha; testar falhas, reexecução, concorrência e regressão; executar smoke HTTP/Mailpit; revisar documentação e criar commit local.

## Registro contínuo

## Inspeção dirigida

- O schema existente possui as migrations V1 a V9; a V9 é `V9__create_admin_post_sale_tables.sql`.
- O envio atual é síncrono e especializado em recuperação de senha, com `CapturedEmailSender` no perfil `test` e `SmtpEmailSender` nos demais perfis.
- O fluxo de recuperação persiste somente o hash do token e os testes extraem o token exclusivamente do capturador de teste.
- `OrderService.create` cria o pedido, reserva estoque e registra `CREATED` na mesma transação; `applyPaymentApproved` confirma reservas e registra `PAYMENT_APPROVED`; `AdminOrderService.ship` registra envio e atualiza o estado logístico.
- Não existe infraestrutura de outbox ou scheduler de notificações comerciais. O scheduler existente expira pedidos; a reconciliação de pagamentos é acionada por serviço/controlador de desenvolvimento.
- O banco de desenvolvimento não foi alterado nesta inspeção.

## Plano técnico adotado

1. Criar migration V10 para outbox persistente e histórico de tentativas.
2. Introduzir mensagem de e-mail genérica sem remover o caminho especializado de recuperação.
3. Enfileirar pedido recebido, pagamento aprovado e pedido enviado dentro das transações que efetivam essas operações.
4. Processar a outbox fora da transação comercial, com `FOR UPDATE SKIP LOCKED`, lease, backoff e estado terminal.
5. Expor somente consulta administrativa sanitizada da outbox; não expor corpo de mensagens ou segredos.
6. Validar com PostgreSQL real, capturador de e-mail, regressão completa e smoke isolado com Mailpit quando disponível.

## Decisão sobre recuperação de senha

A recuperação permanece com transporte especializado nesta fase. O token continua sendo armazenado somente como hash e não é colocado na outbox, porque a entrega posterior exigiria persistir o link recuperável ou um segredo equivalente. A mensagem foi alinhada ao transporte genérico, mas o mecanismo de recuperação mantém a garantia de não persistir o token em texto aberto.

Este documento será atualizado durante a execução. Nenhuma senha, token, cookie, link real de recuperação ou credencial será registrado.

## Implementação realizada

- V10 criou `notifications` e `notification_attempts`, com FK para pedido/cliente, chave de deduplicação, estados, tentativas, lease, backoff, checks e índices.
- `NotificationService` cria `ORDER_RECEIVED`, `PAYMENT_APPROVED` e `ORDER_SHIPPED` dentro das transações comerciais. A chave lógica impede duplicação em checkout repetido, webhook repetido ou envio administrativo idempotente.
- `NotificationDispatcher` reivindica trabalho com `FOR UPDATE SKIP LOCKED`, lease recuperável, limite de tentativas e backoff. O SMTP ocorre fora da transação comercial; falha de SMTP não desfaz pedido, pagamento, reserva ou envio.
- `EmailMessage` suporta texto e HTML. `SmtpEmailSender` usa mensagem multipart UTF-8; o capturador de teste preserva mensagens para asserções.
- Templates neutros em português foram implementados em código, com escape HTML, BRL decimal, datas UTC e identificador estável de mensagem. O corpo da mensagem não é exposto no endpoint administrativo.
- Recuperação de senha permaneceu especializada: o token continua somente como hash e não entra na outbox.
- `GET /api/v1/admin/notifications` permite consulta ADMIN paginada, com whitelist de ordenação e filtros de estado/tipo.
- `compose.phase11-acceptance.yaml` isolou PostgreSQL, volume e Mailpit; o JAR foi executado em porta separada.

## Problemas encontrados e correções

1. A primeira tentativa de fixture SQL ocorreu antes de a aplicação aplicar as migrations e retornou relações inexistentes; o pipeline original ainda terminou com código 0. A fixture foi repetida com `ON_ERROR_STOP=1` depois do boot, criando somente dados sintéticos no banco isolado.
2. Uma inicialização inicial usou Java 8 do PATH e falhou com `UnsupportedClassVersionError`. O launcher foi corrigido para o Zulu JDK 17.0.16 usado pelo Maven Wrapper.
3. O primeiro smoke SMTP registrou `RETRY_PENDING`; a causa foi `MimeMessageHelper` sem modo multipart, incompatível com `setText(text, html)`. A construção foi corrigida para multipart UTF-8, e a execução seguinte entregou as mensagens no Mailpit.
4. O primeiro smoke administrativo reutilizou chaves fixas de idempotência no banco persistente e não avançou o envio. O script de homologação passou a gerar chaves únicas por execução; o pedido seguinte ficou `PREPARING` e depois `SHIPPED`, com `ORDER_SHIPPED` entregue.
5. Uma execução de `clean verify` ficou presa em timeout de readiness do Testcontainers/PostgreSQL. Os processos iniciados pela execução foram encerrados; a repetição, após o ambiente Docker estabilizar, terminou com 81 testes aprovados.
6. O smoke final encontrou estoque sintético esgotado por execuções anteriores e recebeu 409. Foi registrada uma entrada pelo endpoint ADMIN real no banco isolado; o smoke final contra o JAR foi repetido com sucesso.

## Evidências finais

- `NotificationIntegrationTest`: 6 testes, 0 falhas, 0 erros, 0 ignorados; cobre deduplicação, HTML/escape, retry, rollback e claim concorrente.
- Regressão `clean verify` final: 81 testes, 0 falhas, 0 erros, 0 ignorados; Testcontainers PostgreSQL 18.6; JAR `backend/target/perfumestore-backend-0.0.1-SNAPSHOT.jar` gerado.
- Smoke HTTP/Mailpit final: health 200; pedido `PS-81268E923F8947A2A21CF6C9D9EA8409`; estado inicial `PENDING_PAYMENT`; pagamento `APPROVED`; separação `PREPARING`; envio `SHIPPED`; subtotal BRL 55,00; frete BRL 15,90; total BRL 70,90; mensagens de pedido recebido, pagamento aprovado e pedido enviado capturadas; notificações observadas como `SENT`.
- Persistência: após restart do PostgreSQL dedicado e reinício da aplicação, health `UP`, Flyway validou 11 migrations e informou schema na versão 10; 6 pedidos e 15 notificações permaneceram; 15 notificações estavam `SENT`; saldo sintético ficou físico 4, reservado 0, disponível 4.
- O perfil `prod` continua sem controllers MOCK (`@Profile({"local", "test"})`), usa `UnavailablePaymentProvider` fora de local/test e deixa notificações desabilitadas por padrão. Não foi usado provedor financeiro real.

## Estado final e limites

- A homologação local com Mailpit foi aprovada. O Mailpit e o volume PostgreSQL pertencem somente ao projeto de aceitação da Fase 11 e foram preservados para reprodução; os containers serão parados ao final.
- SMTP real, credenciais produtivas, domínio/remetente comercial, observabilidade distribuída e garantia universal de entrega exatamente uma vez permanecem pendentes.
- A integração financeira real e a operação comercial real permanecem fora desta fase. A Fase 12 não foi iniciada.
- O commit local será registrado após a revisão final de diff, segredos e estado do Git.

## Commit e encerramento

- Commit final: criado localmente; o hash definitivo foi verificado no encerramento e é informado no handoff final.
- Mensagem: `feat: implement reliable transactional email notifications`.
- Branch final: `main`.
- Árvore final limpa após o commit; nenhum `origin` configurado; não houve push, deploy ou tag.
- A aplicação e os containers `perfumestore-phase11-acceptance-postgres-1` e `perfumestore-phase11-acceptance-mailpit-1` ficaram parados; o volume nomeado foi preservado.
