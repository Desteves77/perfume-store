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
