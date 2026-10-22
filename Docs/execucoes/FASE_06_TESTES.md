# Fase 6 — Testes e evidências

## Estado inicial

A suíte de referência da Fase 5 tinha 41 testes aprovados. A Fase 6 acrescentará testes de endereço, isolamento entre clientes, CSRF, cotação, configuração e persistência, mantendo PostgreSQL real via Testcontainers.

## Plano de validação

- Endereços: cadastro, normalização de CEP, UF, campos obrigatórios, múltiplos registros, atualização com versão, remoção lógica e listagem.
- Segurança: anônimo, CUSTOMER próprio, IDOR entre dois clientes, proprietário no payload, CSRF e sessão.
- Frete: faixa atendida, fora da faixa, CEP inválido, configuração ausente/inválida, BRL, prazo, validade e ausência de carrinho.
- Regressão: infraestrutura, autenticação, catálogo, estoque, concorrência e migrations.
- Smoke: HTTP real em Compose isolado, reinicialização e preservação de dados.

Os comandos, contagens reais, falhas e reexecuções serão adicionados durante a execução. Nenhum teste será marcado como aprovado sem execução observável.

## Resultados executados

| Grupo | Comando/ambiente | Resultado |
|---|---|---|
| Compilação principal | backend/mvnw.cmd --batch-mode -DskipTests compile | Passou após correção do import do serializador |
| Compilação de testes | backend/mvnw.cmd --batch-mode test-compile | Passou após correções de text block, UTF-8 e helper |
| Teste focado | backend/mvnw.cmd --batch-mode -Dtest=AddressShippingIntegrationTest test | 8 aprovados, 0 falhas, 0 erros, 0 ignorados |
| Regressão/build | backend/mvnw.cmd --batch-mode clean verify | 49 aprovados, 0 falhas, 0 erros, 0 ignorados |
| Smoke HTTP | script local contra Compose isolado | 19 aprovados, 0 falhas |
| Persistência | PostgreSQL isolado antes/depois do restart | 6 endereços ativos, 8 cotações e Flyway versão 5 preservados |

A suíte completa final contém:
- AdminProvisioningIntegrationTest: 2.
- ApiContractIntegrationTest: 12.
- AuthenticationIntegrationTest: 10.
- CatalogIntegrationTest: 6.
- InfrastructureIntegrationTest: 2.
- InventoryIntegrationTest: 8.
- SessionPersistenceIntegrationTest: 1.
- AddressShippingIntegrationTest: 8.
Total: 49 testes aprovados; nenhum teste falhou, produziu erro ou foi ignorado.

## Cenários HTTP do smoke

| Cenário | Resultado |
|---|---|
| Health da aplicação isolada | 200 / UP |
| Cadastro e login de dois clientes sintéticos | 201/200 |
| Cadastro, listagem e consulta do próprio endereço | 201/200 |
| Cliente B consulta, altera e remove endereço de A | 404 em todos; sem alteração |
| Atualização do próprio endereço | 200 |
| Cotação na faixa configurada | 200, BRL 15.90, 3–6 dias úteis |
| Cotação fora da faixa | 422 SHIPPING_DESTINATION_UNSERVED |
| CEP sintaticamente inválido | 422 |
| Cotação autenticada sem CSRF | 403 |
| Remoção lógica e consulta posterior | 204 e 404 |
| Consulta após reinicialização | 200; dados preservados |

O script executou 19 asserções HTTP no total, incluindo health, cadastro, autenticação, isolamento, CSRF, cotação e persistência. Nenhuma credencial, sessão ou token aparece no resultado compartilhável; apenas UUIDs sintéticos foram usados para rastrear o smoke nos logs ignorados.

## Relatórios Maven e logs

Os relatórios JUnit ficam em backend/target/surefire-reports. As saídas longas usadas nesta execução ficam em Docs/execucoes/logs, ignorado pelo Git. Os arquivos de log não foram adicionados ao commit.

## Lacunas e limites

Não existe regra comercial real aprovada para área, tarifa, modalidade ou prazo; os testes usam a configuração sintética documentada. Não houve integração de CEP, transportadora ou cálculo dependente de peso/valor do carrinho. Não houve criação de pedido, checkout ou reserva. O endpoint de cotação exige cliente autenticado e endereço próprio; a consulta pública por CEP sem conta não foi implementada.

## OpenAPI e encerramento

A verificação contra o JAR final retornou 31 caminhos OpenAPI, com os dois endpoints da Fase 6 presentes. O health do artefato retornou status UP. Os containers e a aplicação foram parados após essa verificação, sem remover o volume.
