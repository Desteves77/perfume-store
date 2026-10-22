# Fase 5 — Relatório de implementação de estoque

## Estado inicial

- Diretório: `C:\Users\User\Desktop\Ecommerce PerfumeStore`.
- Branch: `main`.
- HEAD inicial: `61702d0bdea963d53eb4d110ff2c730a93b68135`.
- A árvore estava limpa no início da fase.
- V1–V3 estavam presentes e não foram alteradas.
- Maven Wrapper executava com JDK 17.0.16; o executável `java` do PATH apontava para Java 8.
- Docker estava indisponível nesta sessão no momento da inspeção; os containers existentes não foram parados ou modificados.
- Não havia `origin` configurado.

## Objetivo e plano

Implementar saldo por apresentação, entradas e ajustes administrativos positivos, histórico imutável, serviços internos de reserva/confirmação/liberação, proteção transacional contra concorrência e disponibilidade no catálogo. O plano é: migration V4; repositórios JDBC; serviço transacional; endpoints ADMIN; integração de leitura do catálogo; testes PostgreSQL/HTTP; documentação; verificação e commit.

## Decisões adotadas

- O saldo é inicializado sob demanda com zero quando uma operação administrativa ou consulta administrativa o requer. A migration não cria estoque positivo e a leitura pública trata a ausência de linha como disponibilidade zero.
- `available = physical - reserved`; a disponibilidade não é armazenada como autoridade independente.
- A entrada administrativa exige quantidade positiva; `ADJUSTMENT` aceita delta inteiro não nulo, inclusive negativo, desde que não reduza o físico abaixo do reservado. A baixa física de venda ocorre na confirmação de uma reserva; devoluções e reposição após inspeção dependem de procedimento posterior.
- Reservas são serviços internos, sem endpoint público ou endpoint administrativo de teste. A chave de reserva é persistida e única; estados são `ACTIVE`, `CONFIRMED` e `RELEASED`.
- Não há expiração automática nesta fase. `expires_at` é mantido para compatibilidade futura, mas o ciclo de pedidos/expiração pertence à Fase 8.
- A concorrência usa transação PostgreSQL e `SELECT ... FOR UPDATE` sobre a linha de saldo, com constraints e unicidade como defesa adicional.
- O catálogo público retorna somente `availability.availableQuantity` e `availability.availableForPurchase` por apresentação; não retorna físico, reservado, movimentações, reserva ou administrador.

## Implementação

Arquivos principais: `backend/src/main/resources/db/migration/V4__create_inventory_tables.sql`; os repositórios, DTOs, estados, `InventoryService` e `InventoryAdminController` em `backend/src/main/java/com/perfumestore/backend/inventory/`; a porta `InventoryAvailabilityReader` em `shared/inventory`; e a integração de disponibilidade em `CatalogService`/`CatalogDtos`.

A API administrativa implementada é `GET /api/v1/admin/inventory/presentations/{presentationId}`, `POST /api/v1/admin/inventory/adjustments` e `GET /api/v1/admin/inventory/movements`. Reserva, confirmação e liberação são serviços internos. O catálogo público retorna o recorte `availability` por apresentação.

Os testes focados foram executados com PostgreSQL real via Testcontainers: 8 testes aprovados, sem falhas, erros ou ignorados. A compilação principal e de testes passou. A suíte completa executou 41 testes, sem falhas, erros ou ignorados. O smoke HTTP real no ambiente isolado executou 16 cenários, sem falhas. Após reinicialização da aplicação, o saldo físico 5, reservado 0 e disponível 5, a disponibilidade pública 5 e o histórico de uma movimentação permaneceram persistidos.

## Problemas, correções e limitações

Foram encontrados e corrigidos problemas operacionais durante a execução: uma compilação inicial foi chamada na raiz sem `pom.xml`; um helper de teste colidiu com o import estático de MockMvc; a primeira tentativa de provisionamento usou o caminho incorreto para `.env.phase5`; e o primeiro smoke foi iniciado antes do Mailpit isolado, produzindo health `503`. As correções foram repetir os comandos no diretório correto, renomear o helper, carregar o arquivo a partir da raiz e iniciar somente o Mailpit dedicado. O smoke final passou com health `200`.

Na revisão funcional, a primeira implementação restringia `ADJUSTMENT` a entradas positivas. Isso não cobria a correção física por delta prevista no domínio. A causa foi uma validação global `@Positive`; a correção separou a regra por tipo: `ENTRY` positivo, `ADJUSTMENT` inteiro não nulo, com bloqueio explícito quando o físico ficaria abaixo do reservado. O teste focado e o `clean verify` foram reexecutados, e o smoke HTTP final passou após o novo build.

Na revisão de desempenho do catálogo, a disponibilidade da listagem pública foi consolidada para uma leitura em lote das apresentações da página, evitando uma consulta de saldo por produto. O `clean verify` e o smoke HTTP foram executados novamente contra o JAR final.

## Estado final

Implementação, testes, smoke HTTP, persistência após reinicialização, revisão documental e verificação do diff concluídos. O PostgreSQL e o Mailpit dedicados foram parados com `stop`; o volume e os dados sintéticos permanecem preservados para reprodução. O commit local será `feat: implement inventory management and stock reservations`; o hash final será conferido no Git e informado no relatório do terminal.

## Evidências finais

- `InventoryIntegrationTest`: 8 testes, 8 aprovados, 0 falhas, 0 erros, 0 ignorados.
- `clean verify`: 41 testes, 41 aprovados, 0 falhas, 0 erros, 0 ignorados; JAR executável gerado.
- Migrations: V1, V2, V3 e V4 validadas; a reinicialização registrou o schema na versão 4 sem reaplicação.
- Provisionamento: duas execuções dedicadas concluídas com código 0, sem endpoint público e sem duplicação de ADMIN.
- Smoke HTTP: 16 cenários aprovados, incluindo ADMIN, CUSTOMER, anônimo, CSRF, idempotência, histórico, OpenAPI e disponibilidade pública.
- Persistência: saldo físico 5, reservado 0, disponível 5, disponibilidade pública 5 e uma movimentação preservados após restart.
