# PerfumeStore — Fase 0: decisões e pendências

## 1. Decisões já estabelecidas

Estas decisões vêm da solicitação e dos roadmaps e não devem ser reabertas como escolhas de implementação rotineiras:

- backend em Java/Spring Boot, PostgreSQL, Maven, Flyway, Spring Security e API REST com OpenAPI;
- monólito modular por domínio;
- valores monetários com precisão decimal e configurações/segredos externos ao código;
- uma loja, BRL, perfis `CUSTOMER` e `ADMIN`, preço e estoque por apresentação;
- um provedor de pagamento, checkout hospedado e operação administrativa manual quando necessário;
- backend como autoridade para validações, autenticação, autorização, preços, frete, estoque, pedidos, pagamentos e transições;
- sequência: backend comercial básico, frontend, integrações/refinamento/homologação e liberação do MVP;
- exclusão de cupons, marketplace, fidelidade, avaliações, favoritos, ERP, fiscal automático e relatórios avançados;
- nenhuma implementação de aplicação, migration, endpoint ou frontend nesta Fase 0.

## 2. Recomendações técnicas desta fase

| ID | Recomendação | Motivo | Fase principal |
|---|---|---|---|
| REC-01 | Sessão opaca no servidor em cookie seguro, com sessão persistida e revogável | logout efetivo, revogação simples e menor complexidade que JWT | Fase 3 |
| REC-02 | Cookie de visitante com token aleatório; banco guarda hash | carrinho persistido sem confiar em ID escolhido pelo navegador | Fase 7 |
| REC-03 | Carrinho não reserva estoque; merge soma linhas e bloqueia conflitos sem descarte | preserva intenção do cliente e evita redução silenciosa | Fase 7 |
| REC-04 | Reserva na criação do pedido, baixa física na aprovação válida e conversão da reserva | separa disponibilidade temporária de venda confirmada | Fases 5 e 8 |
| REC-05 | `Idempotency-Key` com fingerprint por usuário/rota para pedido e pagamento | reenvio seguro após timeout e prevenção de duplicidade | Fase 8 |
| REC-06 | Webhook por evento único, assinatura validada e reconciliação de estados desconhecidos | tolera repetição, ordem variável e falhas de rede | Fase 9 |
| REC-07 | Aprovação tardia tenta nova reserva; sem estoque vai para `PAYMENT_REVIEW` e não entrega | impede venda acima da disponibilidade | Fase 9 |
| REC-08 | Snapshot de itens, totais, endereço e frete no pedido | histórico não muda com catálogo ou endereço atual | Fase 8 |
| REC-09 | Cancelamento, reembolso e devolução física são registros separados | dinheiro e produto percorrem processos diferentes | Fase 10 |
| REC-10 | Outbox de notificações com chave de evento e retries | e-mail não desfaz venda e não duplica por reprocessamento | Fase 11 |
| REC-11 | Configuração de frete por área/regra versionada no backend | mantém regra comercial fora do frontend e permite alteração controlada | Fase 6 |
| REC-12 | `audit_logs` append-only para ações administrativas e transições sensíveis | rastreabilidade operacional sem registrar segredos | Fases 5, 8 e 10 |

Estas são recomendações, não decisões comerciais do proprietário. A implementação deve registrar qualquer alteração no documento de decisões e nos contratos afetados.

## 3. Configurações provisórias exclusivas de desenvolvimento

Podem existir somente com guarda de ambiente e sem acesso em produção:

- provedor de pagamento simulado;
- e-mail em caixa local, logger sanitizado ou servidor de testes;
- armazenamento local de imagens;
- valores/áreas de frete fictícios para testes automatizados, identificados como dados de teste;
- relógio controlável e dados seed de catálogo/estoque;
- criação de administrador de desenvolvimento por variável/CLI sem credencial versionada.

Essas configurações não são decisões de operação nem podem aparecer como dados reais no frontend ou na documentação pública.

## 4. Pendências externas e comerciais

| Pendência | Sugestão inicial | Impacto | Bloqueia |
|---|---|---|---|
| Provedor de pagamento | escolher um único provedor com checkout hospedado, webhooks assinados e consulta de reconciliação | define SDK/adaptador, estados, assinatura, refund e sandbox | não bloqueia Fases 1–8; bloqueia homologação da Fase 9 |
| Serviço de e-mail | provedor transacional com sandbox e retry | define templates, remetente, limites e observabilidade | não bloqueia Fases 1–8; bloqueia operação da Fase 11 |
| Armazenamento de imagens | objeto/CDN persistente com URLs controladas | define upload, limites, remoção e cache | não bloqueia modelo; bloqueia integração completa da Fase 4 |
| Hospedagem e domínios | confirmar frontend/API, HTTPS, proxy e domínio registrável comum | altera cookies, CORS, CSRF e callbacks | não bloqueia código inicial; bloqueia homologação real das Fases 12–13 |
| Área atendida | definir cidades/UF/faixas de CEP e exceções | define validação de destino | não bloqueia Fases 1–5; bloqueia frete real na Fase 6 |
| Regra, valores e prazos de frete | aprovar modalidade, tabela e prazo exibido | define total comercial e snapshot do pedido | não bloqueia contrato/protótipo; bloqueia aceite da Fase 6 |
| Prazo de reserva | aprovar duração operacional e política de limpeza | afeta conversão, expiração e pagamentos tardios | não bloqueia Fases 1–7; bloqueia parametrização final da Fase 8/9 |
| Cancelamento e reembolso | documentar responsáveis, SLA, critérios e uso do painel do provedor | define ações ADMIN e mensagens ao cliente | não bloqueia checkout simulado; bloqueia operação da Fase 10 |
| Primeiro administrador | escolher procedimento de provisionamento e responsável | afeta bootstrap seguro da operação | não bloqueia desenvolvimento; bloqueia acesso administrativo de homologação |
| Dados reais da loja | nome, contato, políticas e identidade visual | afeta frontend e publicação | não bloqueia backend; bloqueia lançamento integrado |

Nenhuma conta, serviço ou contratação será criada nesta fase. Onde a decisão externa ainda falta, a documentação usa identificadores genéricos e declara a pendência.

## 5. Pendências de implementação controlada

- Fixar versões e matriz de compatibilidade na Fase 1 após verificação técnica.
- Definir limites de página, upload, tamanho de carrinho, expiração de sessão e retenção de logs.
- Escolher estratégia concreta de busca por nome depois de medir o catálogo inicial.
- Definir contrato OpenAPI executável a partir destes contratos iniciais.
- Definir política de retenção e anonimização de dados pessoais conforme orientação jurídica/operacional.
- Confirmar se o deploy usará um ou mais nós e, portanto, se sessão compartilhada exigirá armazenamento centralizado.

## 6. Conflitos identificados

Não foi encontrado conflito relevante entre os dois roadmaps e o escopo desta fase. Ambos mantêm as regras no backend, tratam pagamento por confirmação do servidor, exigem idempotência no checkout e deixam a escolha de serviços externos para etapas posteriores. Os documentos de roadmap permanecem inalterados.

## 7. Decisões técnicas registradas na Fase 2

- O contrato de erro executável usa `application/problem+json`, código estável, mensagem em português, `correlationId` e lista opcional de erros por campo. Erros de MVC e da cadeia do Spring Security usam o mesmo formato.
- O filtro de correlação valida `X-Correlation-Id`, gera UUID quando necessário, devolve o valor no response header e limpa o MDC no fim da requisição. Corpos, cookies, tokens e autorização não entram no log automático.
- A paginação começa em zero, usa tamanho padrão 20 e máximo 100. Ordenação é uma expressão `campo,direção` validada por whitelist de cada endpoint e recebe desempate estável pelo identificador.
- A serialização monetária é localizada no DTO `MoneyResponse`, usa `BigDecimal` e string decimal em BRL. Não há serializador global que altere números decimais não comerciais.
- A aplicação usa Spring Security com sessão preparada (`IF_REQUIRED`), sem formulário, HTTP Basic, JWT, usuário fixo ou senha gerada. Até a Fase 3 não há `UserDetailsService` autenticável; o bean fail-closed impede a senha automática de desenvolvimento.
- CSRF permanece ativo para a estratégia futura por cookie. `GET /api/v1/security/csrf` emite `XSRF-TOKEN`; o frontend deve enviar `X-XSRF-TOKEN` com `credentials: include`. Esse endpoint técnico não implementa autenticação nem representa uma sessão comercial.
- CORS é explícito por ambiente, com credenciais somente quando configuradas e sem combinação com origem curinga. A origem de desenvolvimento é fallback apenas no perfil local; produção exige `CORS_ALLOWED_ORIGINS`.
- OpenAPI usa `springdoc-openapi-starter-webmvc-ui` 3.1.1, compatível com Spring Boot 4 conforme a documentação oficial do projeto. A documentação fica disponível em local/test e desabilitada em produção.
- O Actuator continua limitado a health, sem detalhes públicos de componentes, ambiente ou configuração.

Essas decisões estabelecem a infraestrutura de contratos e segurança. Elas não antecipam cadastro, sessão persistida, recuperação de senha ou qualquer regra comercial.
## 8. Decisões técnicas registradas na Fase 3

- Sessões comerciais usam Spring Session JDBC com o schema oficial gerenciado pela migration V2. A aplicação usa timeout de inatividade padrão de 30 minutos, limpeza periódica, cookie `SESSION` HttpOnly e `Secure` obrigatório em produção. A escolha evita JWT/Redis nesta topologia de monólito e permite revogação direta.
- Login usa `DaoAuthenticationProvider`, BCrypt com custo 12 e `ChangeSessionIdAuthenticationStrategy`. O controller salva explicitamente o `SecurityContext` no repositório configurado, pois o fluxo de autenticação é implementado em controller próprio.
- A política de senha é de 12 a 72 caracteres e no máximo 72 bytes UTF-8, sem trim ou normalização silenciosa. E-mails são trimados e normalizados com `Locale.ROOT`; não há transformações específicas de provedores.
- Cadastro é público, cria somente `CUSTOMER` e não faz login automático. O primeiro ADMIN é provisionado por runner dedicado e desabilitado por padrão; é idempotente, não promove CUSTOMER e não redefine senha existente.
- Recuperação usa 32 bytes aleatórios, persiste apenas SHA-256 do token, expira em 30 minutos e consome atomicamente. Redefinir senha invalida sessões e tokens ativos e exige novo login. A URL usa somente o frontend configurado, nunca o `Host` da requisição.
- Desenvolvimento usa Mailpit via SMTP local e testes usam capturador em memória. Falha de envio deixa a resposta genérica e remove o token criado; e-mail de produção continua pendente de provedor transacional.
- O limitador de tentativas é local à instância, com limite de login 5/minuto, recuperação 5/15 minutos, redefinição 5/15 minutos e cadastro 10/10 minutos. A resposta é `429` com `Retry-After`. Antes de múltiplas instâncias ou proxy, será necessário armazenamento distribuído e configuração confiável de encaminhamento.

As referências oficiais usadas para a integração são [Spring Session JDBC](https://docs.spring.io/spring-session/reference/guides/java-jdbc.html), [integração HTTP do Spring Session](https://docs.spring.io/spring-session/reference/http-session.html), [persistência de SecurityContext](https://docs.spring.io/spring-security/reference/servlet/authentication/persistence.html) e [gerenciamento de sessão do Spring Security](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/session-management.html).

## 9. Decisões técnicas registradas na Fase 4

- O catálogo usa as tabelas incrementais da V3; a V1 e a V2 permanecem imutáveis. Chaves estrangeiras usam `RESTRICT` para não apagar histórico futuro por cascata.
- Categoria, produto, apresentação e imagem são conceitos separados. Produto e apresentação têm versões para atualização otimista; SKU normalizado é único globalmente e preço é `numeric(12,2)` em BRL.
- Publicação é uma condição derivada: produto e categoria ativos, pelo menos uma apresentação ativa e exatamente uma imagem principal ativa. Categoria desativada oculta os produtos publicados sem reescrever o estado do produto.
- A administração é uma superfície dos domínios sob `/api/v1/admin/**` com `hasRole('ADMIN')`; não foi criado módulo paralelo de regras nem endpoint público de manutenção.
- O adaptador local de imagens implementa `ImageStorage`, gera UUIDs de arquivo, valida path por contenção do diretório e aceita PNG/JPEG decodificáveis. O arquivo é removido quando a transação de metadados falha; remoção administrativa é lógica e mantém a referência para reconciliação/limpeza posterior.
- Na Fase 4, a resposta pública expunha preço e apresentação sem inventar estoque ou disponibilidade. A Fase 5 materializou `availability` com saldo real; o frontend continua sem autoridade para calcular ou alterar esses valores.
- Imagens publicadas podem ser acessadas publicamente por URL controlada; imagens de produtos não publicados somente pelo preview administrativo autorizado. Nenhum caminho físico ou nome original vira URL.

## 10. Pendências após a Fase 4

- Provedor de armazenamento durável/CDN e política de domínio/cache para produção: não bloqueia a implementação local; bloqueia a operação de imagens fora de uma única instância.
- Regras finais de publicação e conteúdo comercial, incluindo eventual categoria múltipla, revisão editorial e auditoria: não bloqueiam a Fase 5, mas exigem decisão antes da homologação comercial.
- Rotina operacional de reconciliação de arquivos órfãos e retenção de imagens removidas: não bloqueia o MVP local; bloqueia a manutenção de produção em escala.
- A tabela de estoque e a representação de disponibilidade foram materializadas na Fase 5 pela V4; nenhuma quantidade positiva fictícia foi adicionada às apresentações existentes.

## 11. Pendências após a Fase 3

- Escolher provedor transacional real, domínio/remetente e política de retry antes da operação; não bloqueia a Fase 4, mas bloqueia homologação real de recuperação.
- Confirmar topologia de domínios e HTTPS para ajustar `SameSite`, `Secure`, CORS e CSRF; não bloqueia desenvolvimento local.
- Definir armazenamento e retenção de auditoria, MFA e procedimento operacional de desabilitação de administradores; não bloqueia o catálogo inicial.
- Substituir o limitador em memória por mecanismo compartilhado antes de escalar horizontalmente; bloqueia a prontidão de produção, não a Fase 4.

## 12. Decisões técnicas registradas na Fase 5

- O estoque é controlado por apresentação, não por produto. A disponibilidade é calculada no backend como físico menos reservado.
- A V4 usa saldo sob demanda com zero para não inventar unidades para apresentações existentes. A primeira entrada materializa o saldo positivo e deixa movimentação auditável.
- A administração aceita entradas positivas e ajustes físicos por delta não nulo. Um ajuste negativo só é aceito quando preserva `physical_quantity >= reserved_quantity`; a baixa física de uma venda continua exclusiva da confirmação de reserva.
- A concorrência usa lock pessimista da linha PostgreSQL, junto de constraints, chave idempotente e histórico na mesma transação. Testes usam transações reais e threads independentes.
- Não há endpoint público de reserva. O serviço interno `InventoryService` será a porta usada futuramente por `order` e `payment`.
- O ciclo da fase limita reservas a `ACTIVE`, `CONFIRMED` e `RELEASED`; expiração automática e integração com pedido ficam para a Fase 8.
- O catálogo expõe quantidade disponível e indicador de compra, sem divulgar físico, reservado, reserva ou ator.

## 13. Pendências após a Fase 5

- Definir a expiração e o agrupamento de reservas no pedido, incluindo reserva de múltiplas apresentações com ordem determinística de locks; bloqueia a implementação do checkout, não a infraestrutura atual.
- Integrar `InventoryService` ao módulo de pedidos para converter reservas após pagamento válido; bloqueia as Fases 8 e 9.
- Definir procedimentos de devolução física, inspeção e eventual reposição; não é substituído por reembolso financeiro.
- Avaliar limite operacional e auditoria externa para saldos muito grandes; a implementação atual limita quantidades a `integer` PostgreSQL/Java.

## 14. Decisões técnicas registradas na Fase 6

- Endereços são recursos privados sob /api/v1/me/addresses; o usuário vem da sessão, nunca do payload. A remoção é lógica e a atualização usa version para evitar sobrescrita silenciosa.
- CEP é normalizado sem hífen e com zeros preservados; UF é validada contra o conjunto brasileiro. Não há confirmação externa de existência do endereço.
- Frete é consultado por cliente autenticado, com CSRF, usando addressId próprio. A regra simples por faixa de CEP, valor BRL e prazo é configurada por ambiente; a cotação persistida guarda validade e versão.
- O perfil local/teste aceita somente parâmetros sintéticos documentados. O perfil de produção não habilita frete por padrão, não tem tarifa/faixa de fallback e falha de forma segura sem configuração.
- A V5 foi criada sem alterar V1–V4; não há tabela de regra comercial administrável nesta fase.

## 15. Pendências após a Fase 6

- Definir a área de atendimento real, modalidade, tarifa, unidade do prazo e prazos efetivos. Bloqueia a homologação comercial e a habilitação do perfil de produção, mas não bloqueia a implementação técnica local.
- Decidir eventual integração de CEP, transportadora e armazenamento de tabela comercial. Fica para a fase de integrações, sem dependência criada agora.
- Integrar a validação de propriedade, recálculo e snapshot de endereço/frete ao carrinho e checkout das fases 7/8.

## 16. Decisoes tecnicas registradas na Fase 7

- O token de visitante e um segredo opaco de 32 bytes aleatorios. Somente SHA-256 e armazenado; o identificador sequencial do carrinho nunca e credencial.
- Carrinho anonimo nao e criado por GET vazio. A primeira mutacao cria o registro e emite `CART_TOKEN`; o TTL anonimo e 30 dias e o autenticado e 90 dias.
- O carrinho autenticado e identificado pela sessao e ha no maximo um carrinho ativo por cliente. O merge do login soma apresentacoes iguais, preserva diferentes e marca o carrinho visitante como `TRANSFERRED`.
- O carrinho nao reserva estoque. Disponibilidade e lida do estoque existente e apresentada como informacao; reserva, confirmacao e liberacao ficam para checkout/pedido.
- Preco vigente e sempre lido do catalogo. A referencia persistida serve somente para avisar mudanca de preco. O subtotal nao inclui frete.
- PostgreSQL fornece locks de linha, unicidade e atualizacao condicional por versao. `Idempotency-Key` evita repeticao de inclusao; conflito da mesma chave com payload diferente retorna `409`.
- A limpeza agendada e local a instancia, em lotes, e pode ser desligada. Em escala horizontal sera necessario coordenar tarefas ou usar mecanismo compartilhado.

## 17. Pendencias apos a Fase 7

- A Fase 8 deve criar pedido, recalcular todos os itens, validar endereco/frete, reservar estoque atomicamente e guardar snapshots comerciais. O carrinho nao e garantia de compra.
- A politica comercial de limites de compra, frete no checkout e prazo de reserva ainda depende das decisoes das fases seguintes.
- Antes de producao, revisar o dominio final do cookie, SameSite, CORS, HTTPS, limpeza coordenada e observabilidade sem tokens.
## Decisões técnicas da Fase 8

- O checkout exige sessão `CUSTOMER`; `ADMIN` não assume identidade de cliente.
- A chave `Idempotency-Key` é persistida por cliente com SHA-256 do payload de endereço/cotação. Reenvio idêntico recupera o resultado; reutilização incompatível retorna conflito.
- A reserva de múltiplas apresentações usa a mesma transação e ordena UUIDs antes de adquirir os locks do inventário.
- O carrinho é esvaziado atomicamente após a criação; em rollback ele permanece disponível.
- O pagamento simulado é condicionado aos perfis `local` e `test`; o perfil `prod` não registra o controller.
- O prazo de pagamento é configurável (`app.order.payment-ttl`) e a expiração é idempotente. O valor local/teste não é política comercial definitiva.

## Pendências

Continuam pendentes o provedor de pagamento real, webhooks, reconciliação, reembolsos e a homologação de condições comerciais reais de frete. A entrega física e a operação administrativa de pós-venda permanecem nas fases posteriores.
## Fase 8 — decisões e pendências

- A criação exige CUSTOMER autenticado, CSRF e `Idempotency-Key`; `addressId` e `quoteId` são os únicos dados de seleção recebidos, e subtotal, frete e total são recalculados no servidor.
- O pedido inicia em `PENDING_PAYMENT`, pagamento em `PENDING` e entrega em `NOT_STARTED`. As apresentações são ordenadas por UUID antes das reservas para reduzir deadlocks.
- Uma linha shell de pedido com `ON CONFLICT` serializa duas criações concorrentes com a mesma chave. O replay com fingerprint igual retorna o pedido original; payload diferente conflita.
- Snapshots de item, endereço e frete preservam o histórico. O carrinho é limpo somente no sucesso transacional.
- O simulador `POST /api/v1/dev/payment/orders/{publicNumber}` está disponível apenas nos perfis `local` e `test`; não há fallback financeiro em produção.
- Prazo de pagamento e expiração são configuráveis. O scheduler usa `ORDER_EXPIRATION_INTERVAL_MS` e a operação comercial real depende de provedor, webhooks, reconciliação e frete real.
## Decisões da Fase 9

- O pagamento foi isolado no domínio `payment`; pedidos continuam donos do ciclo comercial e estoque continua dono de reservas e movimentações.
- O provedor desta fase é `MOCK`, restrito a `local` e `test`. Não há seleção, credencial ou chamada financeira externa.
- Eventos simulados usam HMAC-SHA256 sobre timestamp e corpo bruto, com janela de tolerância configurável. Essa assinatura é somente um mecanismo de teste e será substituída pelo mecanismo do provedor escolhido.
- A unicidade `(provider, provider_event_id)`, locks persistidos e estados transacionais fornecem idempotência entre instâncias. O serviço não depende de cache ou `synchronized` local.
- Valor e moeda do evento devem coincidir com a tentativa. Divergência vai para `REVIEW` sem confirmar ou liberar estoque automaticamente.
- Aprovação após expiração não recria reserva: pagamento fica aprovado, pedido vai para `PAYMENT_REVIEW` e a resolução administrativa futura permanece necessária.
- A fachada do simulador da Fase 8 utiliza o mesmo núcleo de eventos, evitando duplicação da confirmação de estoque.
- O perfil `prod` mantém pagamentos desabilitados sem provedor real; não existe fallback para MOCK.

## Pendências para o provedor real

Continuam pendentes a escolha comercial, credenciais de sandbox/produção, contrato de checkout hospedado, autenticação de webhooks, mapeamento final de estados, política de reconciliação com a API oficial, homologação financeira e liberação comercial. Essas pendências não são resolvidas pelos testes do adaptador MOCK.
## Fase 10 — decisões técnicas e pendências

## Decisoes da Fase 11

- A outbox transacional e persistida em PostgreSQL e deduplicada por `event_key`; nao foi introduzida fila externa.
- O dispatcher utiliza lease e `FOR UPDATE SKIP LOCKED`, processa fora da transacao comercial e recupera registros cujo lease expirou.
- Falhas sao registradas com o codigo sanitizado `EMAIL_DELIVERY_FAILED`; apos o limite configurado, o estado e `FAILED` e o registro permanece para diagnostico.
- SMTP convencional nao oferece exatamente-uma-entrega universal: se o provedor aceitar a mensagem e a aplicacao cair antes de marcar `SENT`, uma retentativa pode gerar duplicidade fisica. A deduplicacao logica permanece garantida no banco.
- Recuperacao de senha nao usa a outbox nesta fase para nao persistir o token recuperavel; mantem hash, expiracao, uso unico e resposta anti-enumeracao.

Permanece pendente a configuracao de um servico SMTP transacional real, observabilidade produtiva e politica operacional de reprocessamento em multiplas instancias. Mailpit e somente local/teste.

Foi adotado controle otimista por `version` do pedido, chaves idempotentes persistidas para ações administrativas e locks transacionais nos registros de pedido, reembolso e devolução. A política do MVP exige rastreamento opcional no envio, não repõe estoque por cancelamento ou reembolso e exige inspeção antes de qualquer `RESTOCK`.

O adaptador MOCK continua restrito a `local`/`test`. Confirmação de reembolso depende de resultado persistido do provedor; escolher um estado manual não comprova devolução financeira. Permanecem pendentes a integração financeira real, política comercial definitiva de cancelamento/devolução e operação produtiva com transportadora.
