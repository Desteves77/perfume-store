# Fase 8 — Relatório técnico

## Estado inicial

- Diretório: `C:\Users\User\Desktop\Ecommerce PerfumeStore`.
- Branch inicial: `main`.
- HEAD inicial: `33e5722f6ea06b3f816a63b7b14f179f51233a8f` (`feat: implement persistent guest and customer carts`).
- A árvore de trabalho estava limpa na inspeção inicial e não havia `origin` configurado.
- O Maven Wrapper utilizou JDK 17.0.16 e Maven 3.9.16 com `JAVA_HOME` configurado para o JDK 17; o `java` fora dessa configuração ainda apontava para Java 8.
- Os containers de desenvolvimento e de homologação da Fase 7 estavam parados e seus volumes foram preservados. Nenhum banco existente foi reinicializado.
- As migrations V1–V6 estavam presentes.

## Objetivo e plano

Implementar o domínio de pedidos sem alterar funcionalidades anteriores: validar carrinho, endereço e cotação; calcular valores no servidor; reservar estoque de forma atômica; persistir snapshots; proteger a criação por idempotência; consultar pedidos; expirar pedidos não pagos; e disponibilizar pagamento simulado somente nos perfis locais/de teste.

O plano de execução é: (1) fechar os contratos reais dos módulos existentes; (2) criar V7 e persistência de pedidos; (3) integrar carrinho, frete, catálogo e inventário; (4) expor checkout, consulta e simulador; (5) adicionar testes PostgreSQL, concorrência, segurança e idempotência; (6) executar smoke HTTP isolado, revisar documentação e criar o commit local.

## Decisões adotadas

- O pedido inicia em `PENDING_PAYMENT`, com pagamento `PENDING` e entrega `NOT_STARTED`.
- A criação do pedido exige `CUSTOMER` autenticado e usa somente o usuário da sessão.
- A chave `Idempotency-Key` é única por cliente e vinculada ao par `addressId`/`quoteId`; uma reutilização com outro payload retorna conflito.
- O estoque é reservado somente dentro da transação de criação do pedido. As apresentações são ordenadas por UUID antes da reserva para reduzir deadlocks.
- O pedido armazena snapshots de itens, endereço e frete; alterações posteriores no catálogo, endereço ou regra de entrega não reescrevem o histórico.
- Após sucesso, os itens do carrinho do cliente são removidos atomicamente. Em falha transacional, o carrinho permanece disponível.
- O vencimento de pedidos não pagos libera reservas de modo idempotente. O prazo continua configurável e não representa uma política comercial real sem configuração explícita.
- O simulador de pagamento será registrado somente nos perfis `local` e `test`; nenhum provedor real será integrado nesta fase.

## Implementações e alterações

Este documento será atualizado durante a execução com os arquivos, migration, testes, problemas, correções, limitações e estado final verificável.

## Pendências

Integração com provedor de pagamento, webhooks, reconciliação e liberação comercial real permanecem na Fase 9. A operação de produção continua condicionada às configurações comerciais de frete já documentadas.

## Estado final

### Implementação concluída

- `V7__create_order_tables.sql` criou pedidos, itens, snapshots de endereço/frete, referências de reservas, histórico e unicidade de idempotência por cliente.
- O módulo `order` recebeu estados separados, DTOs, repositório JDBC, serviço transacional, controller, scheduler de expiração e consultas paginadas.
- O checkout valida carrinho, publicação, preço, endereço, cotação e disponibilidade; calcula valores no servidor; ordena apresentações por UUID; reserva estoque na mesma transação; salva snapshots; limpa o carrinho após sucesso.
- A inserção de uma linha shell antes do snapshot resolve a corrida de duas requisições com a mesma chave. Replay igual retorna o pedido original e fingerprint diferente retorna conflito.
- O simulador local/teste confirma ou libera reservas idempotentemente; aprovação após expiração não recria estoque e permanece em revisão futura.
- Integrações de endereço, frete e carrinho foram estendidas apenas para snapshots/revalidação. Não há pagamento real.

Arquivos principais criados: `backend/src/main/resources/db/migration/V7__create_order_tables.sql`, `backend/src/main/java/com/perfumestore/backend/order/`, `backend/src/main/java/com/perfumestore/backend/payment/PaymentSimulationController.java`, `backend/src/main/java/com/perfumestore/backend/config/OrderProperties.java` e `backend/src/test/java/com/perfumestore/backend/OrderIntegrationTest.java`. Também foram modificados os perfis/configuração, integrações de endereço/frete/carrinho, teste de migration, README, STATUS e documentos backend.

### Problemas e correções verificados

1. A primeira compilação encontrou uma chamada de construtor incompleta no mapeamento de `OrderRecord`; a correção foi aplicada e o compile seguinte passou.
2. O teste de catálogo ainda esperava somente V1–V6; foi atualizado para conferir V7 e as tabelas de pedido.
3. A primeira execução concorrente mostrou que uma requisição com a mesma chave podia observar o carrinho já limpo; a criação do pedido shell com `ON CONFLICT` passou a ocorrer antes do snapshot do carrinho. O teste concorrente passou.
4. A primeira suíte completa não inicializava por uma expressão SpEL que procurava `orderProperties` no `@Scheduled`; o scheduler foi alterado para placeholder de configuração. A regressão passou.
5. O primeiro container PostgreSQL isolado falhou por montar o volume de uma imagem 18.6 em `/var/lib/postgresql/data`; um segundo container/volume isolado usou `/var/lib/postgresql`. O banco de desenvolvimento não foi tocado.
6. Um provisionamento inicial usou senha abaixo do limite e terminou com exit 1 sem alteração; a reexecução com senha externa válida criou o ADMIN sintético e terminou com exit 0.
7. Um primeiro health smoke recebeu 503 por ausência do SMTP local; o smoke foi repetido com Mailpit isolado e o indicador de e-mail desabilitado somente nessa execução. Liveness/readiness e health agregado terminaram 200.

### Verificações finais

- Foco: `OrderIntegrationTest` 6/6; regressão catálogo+pedidos 10/10.
- Regressão/`clean verify`: 64 testes, 0 falhas, 0 erros, 0 ignorados; JAR executável gerado.
- Smoke: pedido `PS-FAFF866DF878428885BBC736E8D02BA3`, subtotal 55,00, frete 15,90, total 70,90 BRL, replay idempotente, aprovação simulada, estoque físico 2/reservado 0/disponível 2, anônimo 401 e escrita sem CSRF 403.
- Persistência: após reiniciar o JAR, health 200, pedido recuperado e carrinho persistido; Flyway validou V1–V7 sem reaplicar.
- Containers de homologação foram parados e os volumes `perfumestore-phase8-data` e `perfumestore-phase8-data-v2` preservados. O PostgreSQL de desenvolvimento permaneceu intacto.
- A consulta final ao `/v3/api-docs` retornou 38.429 bytes e continha os três caminhos de pedidos/checkout; health/liveness/readiness permaneceu UP.

### Estado final da fase

Implementação técnica validada. O commit local desta entrega foi criado com a mensagem `feat: implement checkout and order creation`; a operação comercial real continua pendente de pagamento real/webhooks/reconciliação e configuração comercial real de frete.

Será preenchido após os testes finais, smoke HTTP, revisão do diff e commit.
