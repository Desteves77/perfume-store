# Fase 4.1 — Testes e evidências

## Resumo inicial

| Suíte/verificação | Testes | Falhas | Erros | Ignorados | Estado |
|---|---:|---:|---:|---:|---|
| Suíte existente antes da homologação | 31 | 0 | 0 | 0 | Estado reportado no commit de referência |
| Homologação HTTP isolada | 31 verificações de status/assertivas | 0 | 0 | 0 | Aprovada |
| Persistência após reinicialização | 1 fluxo | 0 | 0 | 0 | Aprovada |
| `clean verify` final | 33 | 0 | 0 | 0 | Aprovada |

## Matriz de cobertura

A tabela abaixo relaciona o teste automatizado existente, a evidência HTTP, lacunas concretas e ações realizadas.

## Evidências

Nenhuma credencial, senha, token, cookie ou dado real será registrado. Relatórios Maven ficam em `backend/target/surefire-reports/`; logs brutos locais ficam em `Docs/execucoes/logs/`.

## Resultados finais

| Suíte/verificação | Testes | Falhas | Erros | Ignorados | Evidência |
|---|---:|---:|---:|---:|---|
| `CatalogIntegrationTest` direcionado | 6 | 0 | 0 | 0 | `phase4-1-catalog-tests.log` |
| `clean verify` completo | 33 | 0 | 0 | 0 | `phase4-1-clean-verify.log` e `backend/target/surefire-reports/` |
| Smoke HTTP isolado | 31 verificações de status/assertivas | 0 | 0 | 0 | `phase4-1-smoke.log` |
| Persistência após reinício | 1 fluxo | 0 | 0 | 0 | `phase4-1-persistence-after.log` |

## Matriz de cobertura da homologação

| Cenário exigido | Teste automatizado existente | Evidência de smoke | Lacuna encontrada | Ação |
|---|---|---|---|---|
| Validação de categoria/produto/apresentação | DTOs e fluxos válidos; validação coberta pela API compartilhada | criação válida aprovada | combinações inválidas completas não foram todas exercitadas por HTTP | mantida cobertura existente; sem alteração funcional |
| SKU duplicado | restrição e tratamento no serviço | não repetido no smoke | sem cenário HTTP dedicado nesta execução | revisão do banco/serviço; pendente como ampliação futura |
| Preço/volume inválidos e escala | novo teste em `CatalogIntegrationTest` | — | lacuna automatizada identificada | teste adicionado; `422 VALIDATION_ERROR` aprovado |
| Produto/apresentação inativos | fluxo existente e novo teste | apresentação inativa oculta; produto inativo retorna `404` | — | aprovado |
| Versão obsoleta | novo teste em `CatalogIntegrationTest` | — | lacuna automatizada identificada | teste adicionado; `409 CONCURRENT_UPDATE` aprovado |
| Paginação e whitelist de ordenação | testes da Fase 4 e contrato compartilhado | filtro/busca e ordenação permitida | limite extremo não repetido no smoke | mantida suíte existente |
| Upload inválido/MIME incompatível | `enforcesBackendPermissionsAndImageContentValidation` | uploads PNG válidos | dimensões/tamanho máximo não exercitados no smoke | mantida validação no código e teste existente de MIME |
| Path traversal | teste existente verifica URL sem caminho físico | nomes sintéticos não influenciam chave | — | aprovado por teste e implementação |
| Reordenação com imagem de outro produto | regra no serviço | reordenação do próprio produto aprovada | caso entre produtos não executado | lacuna registrada para cobertura futura |
| Imagem principal única | índice parcial e fluxo de troca | principal, ordem e remoção não principal aprovados | concorrência de troca não exercitada | mantida restrição PostgreSQL |
| Acesso a arquivo por publicação | teste existente e controller separado | produto inativo: público `404`, ADMIN `200` | — | aprovado |
| Falha entre storage e metadados | limpeza no serviço e logs protegidos | não injetada no smoke | falta de adaptador de falha dedicado | pendente para teste operacional de storage |
| Categoria inativa | novo teste em `CatalogIntegrationTest` | filtro/publicação do produto aprovado com categoria ativa | — | teste adicionado; produto ficou ativo no banco e oculto publicamente |
| Persistência de banco/arquivos | testes Testcontainers e smoke | reinício preservou estados e SHA-256 | — | aprovado |

## Cenários HTTP observados

| Cenário | Esperado | Observado |
|---|---|---|
| Health da aplicação isolada | `200` | `200` |
| Login/perfil ADMIN | `200` | `200` |
| Criações administrativas | `201` | `201` |
| Publicação e alterações | `200`/`204` | aprovadas |
| Catálogo público | `200` | aprovado com busca, filtro, detalhe, preços e imagens |
| Anônimo em administração | `401` JSON | `401` |
| CUSTOMER em administração | `403` JSON | `403` |
| Mutação sem CSRF | `403 CSRF_INVALID` | `403` |
| Produto/apresentação/imagem indisponíveis publicamente | `404` | `404` |
| Preview ADMIN de imagem inativa | permitido pela política | `200` |

Os relatórios XML finais estão em `backend/target/surefire-reports/`. Os logs compartilháveis foram sanitizados; cookies, tokens, senhas e credenciais não aparecem nos documentos versionados.
