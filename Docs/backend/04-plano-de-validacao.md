# PerfumeStore — Fase 0: plano de validação

Este documento define cenários para as fases de implementação. Não há testes de aplicação nesta etapa porque o sistema ainda não foi inicializado. Cada cenário deve gerar evidência apropriada, como teste automatizado, teste de integração com PostgreSQL/provedor de sandbox, revisão de autorização ou verificação operacional.

| ID | Regra verificada | Resultado esperado | Tipo de verificação | Fase |
|---|---|---|---|---|
| VAL-001 | Cliente sem permissão administrativa | `CUSTOMER` recebe `403`/resposta equivalente; nenhuma alteração administrativa ocorre | integração de segurança e autorização | 3 e 10 |
| VAL-002 | Acesso ao recurso de outro cliente | consulta/edição de endereço, carrinho ou pedido de outro usuário não revela dados e não altera o recurso | teste de autorização e isolamento | 3, 6, 7 e 8 |
| VAL-003 | Cadastro público tentando atribuir `ADMIN` | papel recebido é ignorado/rejeitado; conta criada, se válida, é `CUSTOMER` | teste de contrato e segurança | 3 |
| VAL-004 | Recuperação com token expirado ou reutilizado | operação rejeitada sem trocar senha; token continua inválido; resposta não expõe conta | teste de unidade/integração com relógio controlado | 3 |
| VAL-005 | Produto inativo no carrinho | linha permanece identificada como indisponível; checkout é bloqueado; nenhuma remoção silenciosa | teste de domínio e API | 4, 7 e 8 |
| VAL-006 | Alteração de preço antes do checkout | criação retorna `CHECKOUT_REVIEW_REQUIRED` com preço atual; nenhum pedido é criado | teste de integração transacional | 8 |
| VAL-007 | Duas compras concorrentes da última unidade | exatamente uma reserva é aceita; outra falha de forma determinística; estoque não fica negativo | teste concorrente com PostgreSQL | 5 e 8 |
| VAL-008 | Reenvio de criação de pedido | mesma chave e mesmo conteúdo devolvem o pedido original; não há segundo pedido ou reserva | teste de idempotência e falha de transporte simulada | 8 |
| VAL-009 | Mesma chave com conteúdo diferente | retorna `IDEMPOTENCY_KEY_REUSED`; segundo conteúdo não tem efeito | teste de fingerprint | 8 |
| VAL-010 | Timeout após criação de pedido | cliente consulta a chave/pedido e encontra um único resultado; retry não duplica | teste de integração com timeout injetado | 8 |
| VAL-011 | Webhook inválido | assinatura, provedor ou valor inválido é rejeitado e auditado; estado comercial não muda | teste de contrato de integração e segurança | 9 |
| VAL-012 | Webhook duplicado ou fora de ordem | evento duplicado é no-op; evento fora de ordem é registrado e não regride estado válido | teste de máquina de estados e unicidade | 9 |
| VAL-013 | Pagamento com valor divergente | evento não confirma pagamento nem baixa estoque; incidente fica registrado | teste de integração com payload adulterado | 9 |
| VAL-014 | Aprovação após expiração da reserva | com estoque: nova reserva e conversão atômicas; sem estoque: `PAYMENT_REVIEW`, sem separação e com pendência de reembolso | teste temporal/conjunto com webhook tardio | 9 |
| VAL-015 | Cancelamento concorrente com aprovação | lock/ordem transacional produz um único resultado coerente; nunca há baixa e liberação duplicadas | teste concorrente de pagamento/cancelamento | 9 e 10 |
| VAL-016 | Liberação de estoque repetida | primeira liberação altera saldo; repetições são no-op e registradas | teste de idempotência de reserva | 5, 8 e 9 |
| VAL-017 | Reembolso sem devolução física | reembolso pode ser solicitado/confirmado sem incrementar estoque; registro de retorno continua separado | teste de domínio e integração | 10 |
| VAL-018 | Falha de e-mail sem desfazer a venda | pedido/pagamento permanecem; notificação fica pendente com retry e sem segredo no erro | teste de outbox e falha de provedor | 11 |
| VAL-019 | Sessão expirada e logout | sessão expirada recebe `401`; logout revoga a sessão; cookie deixa de permitir recurso privado | teste de segurança e integração | 3 |
| VAL-020 | Carrinhos de visitante e cliente | associação soma linhas deterministicamente; conflito de limite não altera os carrinhos e pede revisão | teste de domínio/API | 7 |
| VAL-021 | Cotação expirada ou destino não atendido | checkout rejeita cotação; nenhum frete inventado é aplicado | teste de regra de frete | 6 e 8 |
| VAL-022 | Alteração de catálogo após pedido | detalhe do pedido mantém nome, SKU, preço, quantidade e endereço históricos | teste de persistência e regressão | 4 e 8 |
| VAL-023 | Ajuste de estoque inválido | motivo e ator obrigatórios; estoque não fica negativo nem abaixo do reservado | teste de domínio/autorização | 5 |
| VAL-024 | Cliente tenta confirmar pagamento pelo retorno do navegador | URL/parâmetros de retorno não alteram pagamento; estado só muda por webhook/reconciliação | teste de contrato e segurança | 9 |
| VAL-025 | Pedido de outro cliente por número válido | resposta não revela itens, preço, endereço ou existência indevida | teste de autorização e enumeração | 8 |
| VAL-026 | Transição de entrega inválida | não é possível entregar sem envio nem enviar sem pedido confirmado/separação válida | teste da máquina de estados | 10 |
| VAL-027 | Reembolso duplicado | mesma chave/referência retorna a solicitação já registrada; provedor não recebe operação duplicada | teste de idempotência de pós-venda | 10 |
| VAL-028 | Imagem ou upload não permitido | MIME/tamanho/conteúdo inválido é rejeitado; nenhum arquivo não autorizado fica publicado | teste de upload e integração de armazenamento | 4 e 12 |
| VAL-029 | Logs e erros sem dados sensíveis | senha, token, cookie, cartão, segredo e stack trace não aparecem em logs/respostas | inspeção automatizada e revisão de logs | 2, 3, 9 e 12 |
| VAL-030 | Cache entre contas | resposta privada de uma conta não é entregue a outra; catálogo público segue política definida | teste de headers, proxy e frontend integrado | 2, 12 e 13 |
| VAL-031 | Reconciliação manual de reembolso sem evidência compatível | operação permanece pendente; somente evidência verificável, compatível em valor/moeda e referência pode confirmar; nenhuma reposição é criada | teste de domínio, autorização e idempotência | 10 |

## Evidência mínima por tipo

- **Unitário**: invariantes de estado, cálculo decimal, fingerprint e transições sem infraestrutura.
- **Integração**: PostgreSQL real ou equivalente de teste, transação, locks, constraints, cookies e autorização.
- **Contrato externo**: assinatura, payloads, estados, timeout e reconciliação em sandbox do provedor.
- **Operacional**: expiração, retry, outbox, restauração e procedimento manual de cancelamento/reembolso.
- **Revisão**: logs, headers, OpenAPI, ausência de segredos e isolamento por usuário.

## Evidências executadas na Fase 2

| ID | Regra verificada | Resultado observado | Tipo de verificação | Fase |
|---|---|---|---|---|
| VAL-032 | JSON malformado e tipo incompatível | `400`, respectivamente `MALFORMED_JSON` e `INVALID_JSON_TYPE`, com correlação | teste MockMvc | 2 |
| VAL-033 | Validação simples e aninhada | `422`, campos como `name` e `nested.label`, mensagens em português | teste MockMvc | 2 |
| VAL-034 | Paginação e ordenação | limite `101` rejeitado; campo de ordenação desconhecido rejeitado; ordenação permitida com `id` como desempate | teste MockMvc | 2 |
| VAL-035 | Dinheiro e datas | dinheiro como string `199.90`/`BRL`, instante UTC e data civil ISO | teste MockMvc | 2 |
| VAL-036 | Correlação e erro interno | header e corpo correlacionados; mensagem genérica sem marcador interno | teste MockMvc e revisão de log | 2 |
| VAL-037 | Autenticação e autorização | rota protegida devolve JSON `401`; cliente autenticado sem papel recebe `403` | teste Spring Security/MockMvc | 2 |
| VAL-038 | CORS | origem permitida e preflight respondem; origem desconhecida é rejeitada com `403` | teste MockMvc | 2 |
| VAL-039 | CSRF | ausência e token inválido devolvem `403 CSRF_INVALID`; token válido permite operação de fixture | teste Spring Security/MockMvc | 2 |
| VAL-040 | OpenAPI, health e Flyway | documentação e health disponíveis no perfil test; PostgreSQL real aplica V1 | teste de integração com Testcontainers | 1 e 2 |

Os testes automatizados foram executados com `ApiContractIntegrationTest` (12 testes, sem falhas após o ajuste da expectativa de origem CORS) e `InfrastructureIntegrationTest` (2 testes). A verificação de produção confirmou a configuração de OpenAPI desabilitada por arquivo de perfil; não houve deploy nem execução em ambiente externo.

## Evidências executadas na Fase 4.1

| ID | Regra verificada | Resultado observado | Tipo de verificação | Fase |
|---|---|---|---|---|
| VAL-060 | Ambiente isolado e provisionamento ADMIN | Compose próprio, volume próprio, ADMIN sintético criado e reexecução idempotente; processo dedicado encerrou | verificação operacional e PostgreSQL | 4.1 |
| VAL-061 | Fluxo HTTP administrativo do catálogo | categoria, produto, duas apresentações, duas imagens, imagem principal, reordenação e publicação aprovados | smoke HTTP com sessão ADMIN e CSRF | 4.1 |
| VAL-062 | Catálogo público | busca, filtro, detalhe, volumes, preços, imagens e ausência de estoque/disponibilidade fictícios aprovados | smoke HTTP anônimo | 4.1 |
| VAL-063 | Permissões e CSRF | anônimo `401`, CUSTOMER `403` em mutações administrativas e mutação sem CSRF `403` | smoke HTTP com sessões separadas | 4.1 |
| VAL-064 | Desativação e acesso a arquivos | apresentação inativa foi ocultada; produto inativo e imagem pública retornaram `404`; preview ADMIN respondeu `200` | smoke HTTP | 4.1 |
| VAL-065 | Persistência após reinicialização | produto, preços, estados, ordem, imagem principal e checksums permaneceram; Flyway V1–V3 validou sem reaplicar | reinicialização de PostgreSQL/aplicação | 4.1 |
| VAL-066 | Escala e concorrência otimista | escala monetária inválida retornou `422`; versão obsoleta retornou `409 CONCURRENT_UPDATE` | integração MockMvc com PostgreSQL | 4.1 |
| VAL-067 | Categoria inativa | produto permaneceu ativo no banco, mas ficou fora do catálogo público | integração MockMvc com PostgreSQL | 4.1 |

O smoke usou somente dados sintéticos. Os cenários de upload inválido, MIME incompatível, ordenação não permitida, limite de imagens e validações complementares continuam cobertos pela suíte automatizada e pela revisão do código da Fase 4; a matriz detalhada da homologação está em `Docs/execucoes/FASE_04_1_TESTES.md`.
## Evidências executadas na Fase 4

| ID | Regra verificada | Resultado observado | Tipo de verificação | Fase |
|---|---|---|---|---|
| VAL-052 | CUSTOMER sem administração | `/api/v1/admin/categories` retorna `403 FORBIDDEN` | MockMvc/Spring Security + PostgreSQL | 4 |
| VAL-053 | Migration de catálogo sem estoque | V3 aplica quatro tabelas de catálogo; não cria `stock`, `carts` ou `orders` | integração PostgreSQL/Testcontainers | 4 |
| VAL-054 | Publicação e ocultação | produto só fica público com categoria ativa, apresentação ativa e imagem principal; produto inativo retorna `404` | MockMvc + PostgreSQL | 4 |
| VAL-055 | Busca e ordenação | busca por nome não diferencia maiúsculas/minúsculas; ordenação fora da whitelist retorna `400 INVALID_SORT` | MockMvc | 4 |
| VAL-056 | Preço e SKU | preço é serializado como `199.90`/`BRL`; SKU duplicado e escala inválida são rejeitados pelo contrato/banco | MockMvc + PostgreSQL | 4 |
| VAL-057 | Upload raster | arquivo PNG com MIME JPEG é rejeitado; extensão/nome não define o caminho armazenado | MockMvc + storage local | 4 |
| VAL-058 | Imagem principal | primeiro upload vira principal; metadados públicos não expõem caminho físico | MockMvc + PostgreSQL | 4 |
| VAL-059 | Concorrência otimista | atualização com `version` antiga retorna `409 CONCURRENT_UPDATE` | integração JDBC/MockMvc | 4 |

O teste `CatalogIntegrationTest` foi executado com PostgreSQL 18.6 real via Testcontainers: 4 testes, 0 falhas, 0 erros e 0 ignorados. A suíte completa posterior deve confirmar a regressão das fases anteriores e o artefato executável.

## Evidências da Fase 5

| ID | Regra verificada | Resultado esperado | Tipo | Fase |
|---|---|---|---|---|
| VAL-068 | Saldo por apresentação | linha sob demanda começa em zero; apresentações são independentes | PostgreSQL/Testcontainers | 5 |
| VAL-069 | Entrada e histórico | entrada positiva atualiza físico/disponível e registra ator, motivo e saldos | MockMvc + PostgreSQL | 5 |
| VAL-070 | Ajuste inválido/idempotência | quantidade não positiva é rejeitada; reenvio não duplica; chave divergente retorna 409 | integração transacional | 5 |
| VAL-071 | Reserva acima da disponibilidade | 409 `INSUFFICIENT_STOCK`; nenhum saldo ou histórico parcial | PostgreSQL | 5 |
| VAL-072 | Reserva da última unidade concorrente | exatamente uma aceita; saldo nunca fica negativo | threads + PostgreSQL real | 5 |
| VAL-073 | Confirmação e liberação repetidas | efeito físico/reservado ocorre uma vez; terminais incompatíveis retornam 409 | PostgreSQL | 5 |
| VAL-074 | Terminais concorrentes | uma transição prevalece; um movimento terminal é persistido | threads + locks PostgreSQL | 5 |
| VAL-075 | Autorização e CSRF | ADMIN autorizado; CUSTOMER/anônimo e escrita sem CSRF rejeitados | MockMvc/Security | 5 |
| VAL-076 | Catálogo e disponibilidade | quantidade real aparece no recorte público sem físico/reservado; produto sem estoque continua publicado | MockMvc + PostgreSQL | 5 |
| VAL-077 | Migration e regressão | V4 aplica após V1–V3 e a suíte anterior permanece verde | clean verify | 5 |

### Resultados da execução da Fase 5

`InventoryIntegrationTest` executou 8 testes com 8 aprovações, 0 falhas, 0 erros e 0 ignorados, incluindo concorrência real no PostgreSQL. `clean verify` executou 41 testes com 41 aprovações, 0 falhas, 0 erros e 0 ignorados. O smoke HTTP no banco isolado executou 16 cenários aprovados e a verificação após restart preservou físico 5, reservado 0, disponível 5, disponibilidade pública 5 e uma movimentação. V1–V4 foram validadas pelo Flyway sem reaplicação na reinicialização. Os detalhes operacionais estão em `Docs/execucoes/FASE_05_TESTES.md`.

## Evidências executadas na Fase 3

| ID | Regra verificada | Resultado observado | Tipo de verificação | Fase |
|---|---|---|---|---|
| VAL-041 | Cadastro e papel | cadastro válido retorna `201`, hash não aparece, papel é `CUSTOMER` mesmo com tentativa de `ADMIN` | MockMvc + PostgreSQL Testcontainers | 3 |
| VAL-042 | E-mail duplicado e normalizado | e-mail normalizado viola unicidade e retorna `409 EMAIL_ALREADY_REGISTERED` | MockMvc + restrição PostgreSQL | 3 |
| VAL-043 | Login e permissão | credencial inválida é genérica; CUSTOMER não acessa fixture ADMIN; perfil só altera nome | MockMvc/Spring Security | 3 |
| VAL-044 | Sessão persistida e rotação | cookie muda no login, sessão existe em `spring_session`, requisição HTTP seguinte acessa `/auth/me` | HTTP real + PostgreSQL Testcontainers | 3 |
| VAL-045 | Logout | sessão e contexto são revogados; cookie antigo não acessa recurso privado; logout repetido é seguro | MockMvc e HTTP real | 3 |
| VAL-046 | Recuperação genérica | e-mail desconhecido e falha do SMTP retornam `202` sem indicar existência | MockMvc + capturador de e-mail | 3 |
| VAL-047 | Token de recuperação | apenas hash fica no banco; token válido é de uso único; expirado/reutilizado é rejeitado | MockMvc + PostgreSQL | 3 |
| VAL-048 | Concorrência de redefinição | duas utilizações simultâneas do mesmo token produzem somente um sucesso | chamadas concorrentes + PostgreSQL | 3 |
| VAL-049 | Invalidação de senha | senha antiga falha, nova senha funciona e sessões anteriores são removidas | HTTP real + PostgreSQL | 3 |
| VAL-050 | Rate limit | excesso de login retorna `429` e `Retry-After`, sem bloqueio permanente da conta | MockMvc | 3 |
| VAL-051 | Provisionamento ADMIN | runner dedicado cria ADMIN sem senha fixa, é idempotente e não promove CUSTOMER | Spring Boot + PostgreSQL | 3 |

Os comandos, quantidades e relatórios de teste desta execução estão registrados em `Docs/execucoes/FASE_03_TESTES.md`. O smoke com Mailpit deve ser registrado nessa mesma evidência após a execução local.

## Evidências executadas na Fase 6

| ID | Regra verificada | Resultado observado | Tipo de verificação | Fase |
|---|---|---|---|---|
| VAL-078 | Cadastro e normalização de endereço | CEP com hífen foi persistido com oito dígitos, UF em maiúsculas e campos inválidos retornaram 422 | PostgreSQL/Testcontainers + MockMvc | 6 |
| VAL-079 | Isolamento de endereços | cliente B recebeu 404 ao consultar, atualizar ou remover endereço de A; nenhuma alteração ocorreu | integração de segurança/IDOR | 6 |
| VAL-080 | Remoção e concorrência de endereço | remoção lógica ocultou o registro; versão obsoleta retornou 409 CONCURRENT_UPDATE | integração JDBC/MockMvc | 6 |
| VAL-081 | Cotação atendida | valor foi calculado no backend como string BRL e prazo foi retornado em dias úteis | PostgreSQL/Testcontainers + MockMvc | 6 |
| VAL-082 | Destino não atendido e configuração | faixa fora da política retornou 422; configuração incompleta foi rejeitada sem oferecer frete | integração de serviço | 6 |
| VAL-083 | Sessão e CSRF no frete | anônimo recebeu 401; cliente sem CSRF recebeu 403; cotação válida foi aceita | MockMvc/Spring Security | 6 |
| VAL-084 | Persistência e migration | seis endereços ativos e oito cotações permaneceram após reinício; Flyway validou V1–V5 sem reaplicar | Compose/PostgreSQL | 6 |

AddressShippingIntegrationTest executou 8 testes aprovados. A suíte clean verify executou 49 testes aprovados, sem falhas, erros ou ignorados. O smoke HTTP isolado executou 19 cenários aprovados; os detalhes e comandos reais estão em Docs/execucoes/FASE_06_TESTES.md e FASE_06_COMANDOS.md.

## Evidencias executadas na Fase 7

| ID | Regra verificada | Resultado observado | Tipo | Fase |
|---|---|---|---|---|
| VAL-085 | Carrinho visitante persistente | GET sem cookie retornou vazio; primeira inclusao emitiu cookie opaco e GET seguinte recuperou a linha | HTTP isolado + PostgreSQL | 7 |
| VAL-086 | Operacoes do carrinho | inclusao de duas apresentacoes, PATCH absoluto e DELETE retornaram representacao/204 conforme contrato | HTTP isolado | 7 |
| VAL-087 | Preco e disponibilidade | preco foi alterado no banco e a consulta informou `PRICE_CHANGED`; estoque zerado informou `OUT_OF_STOCK` | HTTP isolado + PostgreSQL | 7 |
| VAL-088 | Merge e logout | login transferiu carrinho visitante, logout preservou carrinho da conta e novo login o recuperou | HTTP isolado + teste de integracao | 7 |
| VAL-089 | Isolamento e CSRF | CUSTOMER B recebeu carrinho vazio; teste automatizado cobriu CSRF e isolamento | HTTP isolado + PostgreSQL | 7 |
| VAL-090 | Concorrencia e idempotencia | inclusoes e claims concorrentes mantiveram uma linha/transferencia; chave repetida nao duplicou; chave com payload diferente conflita | PostgreSQL/Testcontainers | 7 |
| VAL-091 | Expiracao e limpeza | carrinho expirado foi marcado e removido apos retencao; outros dominios permaneceram | PostgreSQL/Testcontainers | 7 |
| VAL-092 | Migration e regressao | V6 validou V1-V6 sem reaplicacao; clean verify executou 57 testes sem falhas | Flyway + Maven | 7 |
| VAL-093 | Persistencia apos restart | aplicacao e PostgreSQL isolados foram reiniciados; login, carrinho, itens e Flyway permaneceram validos | HTTP + Compose/PostgreSQL | 7 |

`CartIntegrationTest` executou 9 testes aprovados, incluindo claim concorrente. A suite completa executou 58 testes aprovados, 0 falhas, 0 erros e 0 ignorados. O smoke HTTP executou 18 cenarios aprovados; a terceira execucao apos correcao do roteiro confirmou o resultado. A validacao do OpenAPI respondeu 200 e publicou os quatro caminhos reais de carrinho.

## Evidências executadas na Fase 8

- `OrderIntegrationTest`: 6 cenários com PostgreSQL real, incluindo snapshots, rollback de reservas, autorização/IDOR, expiração, idempotência concorrente e disputa da última unidade.
- Regressão completa: 64 testes, 0 falhas, 0 erros, 0 ignorados; `clean verify` aprovou e gerou o JAR executável.
- Smoke HTTP isolado contra o JAR: health 200, carrinho visitante com cookie, login/claim, endereço, cotação, subtotal R$ 55,00, frete R$ 15,90, total R$ 70,90, replay idempotente, aprovação simulada, 401 anônimo, 403 sem CSRF e persistência após reinicialização.
- Migrations V1–V7 validadas no banco isolado; V1–V6 não foram alteradas e o banco de desenvolvimento não recebeu escritas.
## Fase 8 — evidências de validação

Foram adicionados testes de PostgreSQL real para criação com snapshots e reservas, idempotência sequencial e concorrente, rollback de múltiplas reservas, disputa da última unidade, expiração/liberação, aprovação simulada, isolamento HTTP e CSRF. A migration V7 foi aplicada em bancos Testcontainers novos. A contagem final e o smoke da fase são registrados em `Docs/execucoes/FASE_08_TESTES.md` após a execução completa.
## Evidências planejadas e executadas da Fase 9

| ID | Regra | Verificação |
|---|---|---|
| VAL-094 | Tentativa e sessão MOCK persistidas | `PaymentIntegrationTest`, PostgreSQL/Testcontainers |
| VAL-095 | Assinatura HMAC e corpo adulterado | `PaymentIntegrationTest`, MockMvc sem sessão |
| VAL-096 | Valor/moeda divergentes não confirmam estoque | `PaymentIntegrationTest`, estado `REVIEW` |
| VAL-097 | Aprovação válida confirma reserva uma vez | `PaymentIntegrationTest`, histórico e saldo PostgreSQL |
| VAL-098 | Evento duplicado concorrente | `PaymentIntegrationTest`, duas transações independentes e unicidade V8 |
| VAL-099 | Aprovação tardia | `PaymentIntegrationTest`, pedido `PAYMENT_REVIEW`, reserva não recriada |
| VAL-100 | Reconciliação idempotente | `PaymentIntegrationTest`, estado MOCK persistido e reconsulta |
| VAL-101 | Simulador legado sem fluxo duplicado | `OrderIntegrationTest`, regressão do contrato da Fase 8 |
| VAL-102 | MOCK ausente em produção | inspeção de profiles e smoke do artefato com `prod` |
| VAL-103 | Migration V8 incremental | Flyway em PostgreSQL real, banco novo e suíte completa |

O provedor real, seus webhooks e a homologação financeira permanecem fora da validação desta fase.
