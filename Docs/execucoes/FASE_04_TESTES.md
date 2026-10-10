# Fase 4 — testes e evidências

Relatórios Maven ficam em `backend/target/surefire-reports/`. Logs brutos ficam em `Docs/execucoes/logs/`, ignorado pelo Git. Nenhum registro compartilhável contém senha, token, cookie, credencial ou dado real.

## Resumo

| Suíte | Testes | Falhas | Erros | Ignorados | Evidência |
|---|---:|---:|---:|---:|---|
| Regressão Fases 1–3 antes da revisão | 27 | 0 | 0 | 0 | `backend/target/surefire-reports/`, `phase4-current-verify.log` |
| Catálogo | 4 | 0 | 0 | 0 | `CatalogIntegrationTest`, `phase4-catalog-tests-2.log` |
| Compilação de testes | — | — | — | — | `phase4-test-compile-3.log` |
| Verify final após correção do runner | 31 | 0 | 0 | 0 | `phase4-final-verify.log` |

## Cenários executados

| ID | Regra | Evidência | Resultado |
|---|---|---|---|
| F4-001 | CUSTOMER não administra catálogo | `CatalogIntegrationTest.enforcesBackendPermissionsAndImageContentValidation` | `403 FORBIDDEN` |
| F4-002 | Categoria/produto válidos | `adminPublishesCatalogAndPublicApiHidesInactiveResources` | `201` e persistência PostgreSQL |
| F4-003 | Publicação exige apresentação e imagem principal | mesmo teste, `PUT /status` | publicação `200`, `publishable=true` somente após completar dados |
| F4-004 | Busca, filtro, paginação e ordenação | `publicProducts`, `rejectsUnallowedPublicOrdering` | busca `200`; ordenação não permitida `400 INVALID_SORT` |
| F4-005 | SKU, preço e volume | criação de apresentação e asserção `price.amount` | preço `199.90`/BRL |
| F4-006 | MIME declarado divergente do conteúdo | upload PNG declarado JPEG | `415 IMAGE_CONTENT_TYPE_MISMATCH` |
| F4-007 | Imagem principal e URL controlada | primeiro upload | `primary=true`; URL sem caminho físico |
| F4-008 | Produto inativo fora do catálogo | desativação após consulta pública | detalhe público retorna `404` |
| F4-009 | V3 sem estoque/carrinho/pedidos | consulta JDBC no teste | quatro tabelas catalogadas; nenhuma tabela posterior |
| F4-010 | Migrations preservadas | Flyway em Testcontainers e Compose | V1/V2 não alteradas; V3 incremental |

## Revisão da Fase 3

Foi encontrada e corrigida a incompatibilidade entre o perfil `provision-admin` sem contexto web e a configuração `SecurityConfig` que exige `HttpSecurity`. O runner agora encerra o processo dedicado após concluir; a regra de primeiro ADMIN existente continua impedindo promoção/redefinição. O smoke não pôde autenticar como o ADMIN já existente porque nenhuma credencial administrativa foi fornecida, e nenhuma credencial foi inferida ou alterada.

## Smoke local

- Aplicação: iniciou com Java 17 e Flyway validou V1–V3.
- PostgreSQL: Compose saudável em `127.0.0.1:54329`.
- Health: `GET /actuator/health` respondeu HTTP `200` depois de iniciar Mailpit, pois o starter de e-mail participa do health aggregate local.
- Fluxo administrativo/publicado: bloqueado pela ausência de credencial do ADMIN já existente no banco persistente. Os fluxos HTTP administrativos e públicos equivalentes foram verificados nos testes de integração com PostgreSQL real.
- Não houve exclusão, limpeza de volume ou alteração de dados preexistentes para contornar o bloqueio.

## Localização dos relatórios

- `backend/target/surefire-reports/TEST-com.perfumestore.backend.CatalogIntegrationTest.xml`
- `backend/target/surefire-reports/TEST-com.perfumestore.backend.ApiContractIntegrationTest.xml`
- `backend/target/surefire-reports/TEST-com.perfumestore.backend.AuthenticationIntegrationTest.xml`
- `backend/target/surefire-reports/TEST-com.perfumestore.backend.AdminProvisioningIntegrationTest.xml`
- `backend/target/surefire-reports/TEST-com.perfumestore.backend.SessionPersistenceIntegrationTest.xml`
