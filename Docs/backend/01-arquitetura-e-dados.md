# PerfumeStore — Fase 0: arquitetura e modelo de dados

Status: proposta técnica para iniciar a Fase 1. Versões de Java, Spring Boot, PostgreSQL, Maven e bibliotecas serão verificadas e fixadas na inicialização; este documento não declara versões atuais ou suportadas.

## 1. Arquitetura

O backend será um monólito modular em uma aplicação Spring Boot, com módulos organizados por domínio e contratos internos explícitos. Cada módulo possui casos de uso, regras e persistência próprias; integrações passam por portas/adaptadores. A superfície administrativa chama os mesmos casos de uso de catálogo, estoque, pedido e pós-venda com autorização `ADMIN`, sem duplicar regras em um módulo “admin”.

### Módulos e responsabilidades

| Módulo | Responsabilidades | Dependências permitidas |
|---|---|---|
| `auth` | sessão, login, logout, recuperação de senha, proteção de autenticação | `user`, compartilhado |
| `user` | conta, papel, estado, dados permitidos | compartilhado |
| `address` | endereços do cliente e titularidade | `user`, compartilhado |
| `catalog` | categorias, produtos, apresentações, imagens e publicação | compartilhado |
| `inventory` | saldos, movimentações, reservas e concorrência | `catalog`, `user`, compartilhado |
| `cart` | carrinho de visitante/cliente, linhas e associação | `catalog`, compartilhado; lê disponibilidade sem reservar |
| `shipping` | área, cotação, validade e regra de frete | `address`, `cart`, compartilhado |
| `order` | checkout, pedido histórico, idempotência e transições | `cart`, `catalog`, `inventory`, `shipping`, `user`, compartilhado |
| `payment` | tentativas, checkout hospedado, webhooks e reconciliação | `order`, portas de `inventory`, compartilhado |
| `notification` | outbox, e-mail, tentativas e deduplicação | compartilhado e eventos de domínio |
| compartilhado | dinheiro, relógio, erros, correlação, paginação, eventos técnicos | nenhum domínio |

Dependências entre módulos devem apontar para interfaces de aplicação ou eventos, evitando ciclos de implementação. `payment` não atualiza tabelas de estoque diretamente: solicita um caso de uso transacional de `inventory` associado ao pedido. `notification` não desfaz uma operação comercial.

### Transações e concorrência

Cada caso de uso define sua fronteira transacional. Criação de pedido, reserva, conversão, liberação, ajuste de estoque e aplicação de webhook com efeito comercial são operações transacionais. O lock da linha de estoque deve ocorrer em ordem determinística por apresentação para reduzir deadlocks. A atualização condicional e uma restrição de unicidade são a defesa final contra corrida.

Operações externas ao banco, como criação de checkout ou envio de e-mail, não devem permanecer abertas dentro de uma transação longa. O sistema persiste a intenção e o resultado possível, usa outbox quando necessário e permite reconciliação. Um `version` otimista em pedido, carrinho e entidades administrativas ajuda a detectar edição concorrente.

## 2. Modelo relacional proposto

Os nomes abaixo são nomes lógicos; migrations e nomes físicos serão definidos na Fase 1. UUID é a recomendação para identificadores internos. IDs públicos de pedido devem ser opacos e não sequenciais.

| Entidade | Campos essenciais | Restrições e observações |
|---|---|---|
| `users` | `id`, e-mail normalizado, `password_hash`, `role`, `status`, timestamps, `version` | e-mail único case-insensitive; cadastro público sempre cria `CUSTOMER` |
| `user_sessions` | `id`, `user_id`, `token_hash`, `created_at`, `last_seen_at`, `expires_at`, `revoked_at` | token bruto nunca persistido; hash único; FK para usuário |
| `password_reset_tokens` | `id`, `user_id`, `token_hash`, `expires_at`, `used_at`, `created_at` | hash único; uso único; índice por usuário e validade |
| `addresses` | `id`, `user_id`, nome do destinatário, logradouro, número, complemento, bairro, cidade, UF, CEP normalizado, referência | FK; acesso sempre filtrado por usuário; exclusão lógica ou retenção conforme política |
| `categories` | `id`, `slug`, `name`, `active`, timestamps | slug único; não apagar se houver produto histórico |
| `products` | `id`, `slug`, nome, marca, descrição, `category_id`, `active`, timestamps, `version` | slug único; FK de categoria; desativação lógica |
| `product_presentations` | `id`, `product_id`, `sku`, volume decimal, unidade, nome de exibição, `price`, `currency`, `active`, timestamps, `version` | SKU único; preço `NUMERIC(12,2)`; moeda deve ser `BRL` |
| `product_images` | `id`, `product_id`, chave/URL, alt text, posição, principal, `deleted_at` | índice por produto/posição; no máximo uma principal ativa por produto |
| `inventory_balances` | `presentation_id`, físico inteiro, reservado inteiro, `version`, timestamps | PK/FK da apresentação; `physical >= 0`, `reserved >= 0`, `reserved <= physical` |
| `inventory_movements` | `id`, apresentação, tipo, delta, motivo, referência, ator, `created_at` | imutável; referência operacional única quando aplicável |
| `stock_reservations` | `id`, `order_id`, apresentação, quantidade, estado, `expires_at`, timestamps | quantidade positiva; estado terminal impede nova liberação; índice por vencimento |
| `carts` | `id`, `user_id` opcional, `guest_token_hash` opcional, estado, `expires_at`, `last_activity_at`, `version` | exatamente um titular; token hash único; uma referência ativa por usuário/token |
| `cart_items` | `cart_id`, `presentation_id`, quantidade, timestamps | PK composta; quantidade inteira positiva; uma linha por apresentação |
| `shipping_quotes` | `id`, carrinho/usuário, impressão da requisição, destino resumido, método, valor, prazo, versão da regra, expiração | não é autoridade após expirar; pode ser retida para auditoria curta |
| `orders` | `id`, `order_number`, `user_id`, estado, subtotal, frete, total, moeda, chave/fingerprint de idempotência, `version`, timestamps | número único; valores `NUMERIC(12,2)`; chave de idempotência única por usuário/rota |
| `order_items` | pedido, número da linha, apresentação opcional, SKU, produto, marca, volume/unidade, preço unitário, quantidade, total da linha | snapshot obrigatório; FK comercial pode ser nula com `ON DELETE RESTRICT` ou preservada logicamente |
| `order_delivery_snapshots` | pedido, destinatário, endereço completo, método, área, valor, prazo e versão da regra | um por pedido; não depende de endereço vivo |
| `payment_attempts` | `id`, pedido, provedor, referência externa, checkout URL, estado, valor, moeda, timestamps | referência do provedor única; não guardar cartão; tentativa pode ficar `UNKNOWN` |
| `payment_events` | provedor, `provider_event_id`, tipo, ocorrido em, hash/payload sanitizado, processamento, timestamps | evento único; eventos repetidos são no-op; não guardar dados sensíveis desnecessários |
| `shipments` | pedido, estado, transportadora, rastreio, datas, observações | inicialmente zero ou um por pedido; rastreio pode ser único quando informado |
| `order_cancellations` | pedido, motivo, origem, estado, solicitante, aprovador, datas | registra solicitação e decisão; não substitui reembolso |
| `refunds` | pedido/tentativa, valor, motivo, estado, referência externa, evidência de reconciliação, solicitante, confirmado em | solicitação, evidência e confirmação separadas; moeda e valor conferidos; ação manual não confirma sem evidência compatível |
| `returns` | pedido, itens/quantidades, motivo, estado, recebido em, inspeção, decisão de reposição | devolução física separada de reembolso; só inspeção gera movimentação de retorno |
| `pending_notifications` | evento único, destinatário, template, estado, tentativas, próximo retry, erro sanitizado | outbox/deduplicação; sem tokens ou segredos no erro |
| `audit_logs` | ator, ação, recurso, recurso ID, resultado, correlação, metadados minimizados, timestamp | append-only; não registrar senha, cartão, token ou payload sensível |

Não é necessário criar uma tabela para cada enum, unidade ou valor monetário. Estados podem ser enums controlados na aplicação e restrições no banco; regras que exigirem configuração comercial devem ter tabela/configuração versionada. `order_delivery_snapshots` é separado para tornar explícita a cópia histórica; `returns` é separado para impedir que um reembolso seja interpretado como reposição.

Cancelamento com reserva libera apenas a reserva ainda ativa, uma vez. Pagamento aprovado não repõe estoque automaticamente após cancelamento. Se a mercadoria não foi enviada e a operação confirmar que continua fisicamente disponível, uma movimentação explícita pode registrar a reposição; se foi enviada, só uma devolução recebida e inspecionada pode decidir se volta ao estoque vendável. A reconciliação de reembolso registra evidência e resultado financeiro, mas nunca cria movimentação de estoque. Todas essas referências são idempotentes.

## 3. Integridade, histórico e formatos

- IDs internos usam UUID; timestamps são armazenados em UTC (`TIMESTAMP WITH TIME ZONE` ou equivalente) e convertidos para apresentação local.
- Dinheiro usa `NUMERIC(12,2)` e `currency CHAR(3)`, com `BRL` como única moeda do MVP. JSON usa strings, por exemplo `"199.90"`.
- Quantidades de estoque e linhas são inteiros positivos no domínio; saldos não podem ficar negativos.
- Preços vigentes ficam na apresentação; preços históricos ficam no item do pedido. Cotações e snapshots preservam versão da regra de frete.
- Produto, apresentação, categoria e imagem com referência comercial não são apagados fisicamente. O histórico financeiro e de estoque é append-only, com correções por nova movimentação.
- FK deve impedir órfãos; onde a retenção histórica exigir, a referência comercial é anulável e o snapshot continua obrigatório.
- Limites de upload, tamanho de texto, quantidade máxima de linhas e tamanho de página são configurações validadas no servidor.

## 4. Índices iniciais

1. `users(lower(email))` para login e unicidade.
2. `user_sessions(token_hash)`, `password_reset_tokens(token_hash)` e índices de expiração para limpeza.
3. `products(active, category_id, id)` e busca por nome/slug conforme capacidade do PostgreSQL; a busca textual deve ser medida antes de escolher extensão.
4. `product_presentations(product_id, active)` e `product_presentations(sku)`.
5. `cart_items(cart_id)`, `carts(user_id, state)` e `carts(guest_token_hash, state)`.
6. `stock_reservations(state, expires_at)` e `inventory_movements(presentation_id, created_at)`.
7. `orders(user_id, created_at desc)`, `orders(order_number)`, `orders(state, created_at desc)`.
8. `payment_events(provider_event_id)`, `payment_attempts(order_id, created_at desc)`.
9. `pending_notifications(state, next_attempt_at)` e `audit_logs(resource_type, resource_id, created_at desc)`.

Os índices serão confirmados com consultas reais e planos na implementação; não devem substituir medição.

## 5. Estados e transições

### 5.1 Pedido

| Origem | Destino | Evento/quem | Pré-condições | Estoque | Repetição |
|---|---|---|---|---|---|
| — | `AWAITING_PAYMENT` | criação de pedido / cliente | carrinho, endereço, frete, preço e reserva válidos | cria reserva ativa | mesma idempotência devolve o mesmo pedido |
| `AWAITING_PAYMENT` | `CONFIRMED` | pagamento aprovado / webhook válido | valor/moeda conferidos e reserva convertida | baixa físico e reservado uma vez | no-op se já confirmado |
| `AWAITING_PAYMENT` | `PAYMENT_EXPIRED` | expiração de tentativa/reserva / tarefa | sem pagamento confirmado | libera reserva uma vez | no-op após estado terminal |
| `AWAITING_PAYMENT` | `CANCELED` | cancelamento / cliente ou admin conforme política | reserva liberada ou inexistente | libera reserva ativa | no-op se já cancelado |
| `PAYMENT_EXPIRED` | `CONFIRMED` | aprovação tardia / webhook válido | nova reserva atômica disponível | converte nova reserva | evento repetido no-op |
| `PAYMENT_EXPIRED` | `PAYMENT_REVIEW` | aprovação tardia / webhook válido | pagamento aprovado, estoque insuficiente ou conflito | nenhuma venda é confirmada | mantém pendência e não separa |
| `CONFIRMED` | `SEPARATING` | início da separação / admin | pagamento confirmado e estoque convertido | nenhuma alteração | repetição no-op |
| `CONFIRMED` ou `SEPARATING` | `CANCELED` | cancelamento / admin | não enviado; decisão de reposição registrada | não repõe automaticamente; movimentação separada se fisicamente confirmada | decisão repetida no-op |
| `SEPARATING` | `SHIPPED` | expedição / admin | dados operacionais mínimos e remessa registrada | nenhuma alteração | mesmo rastreio no-op; conflito retorna 409 |
| `SHIPPED` | `DELIVERED` | confirmação de entrega / admin ou integração autorizada | envio existente | nenhuma alteração | no-op |
| `SHIPPED` ou `DELIVERED` | `RETURN_REQUESTED` | solicitação pós-venda / cliente ou admin | motivo e itens informados | nenhuma alteração | retorna solicitação existente |

`PAYMENT_REVIEW` é deliberadamente bloqueante: o pagamento pode estar aprovado, mas não há direito de separar ou entregar até que estoque/refundação seja resolvido.

### 5.2 Pagamento

| Origem | Destino | Evento/quem | Pré-condições | Efeito | Repetição |
|---|---|---|---|---|---|
| — | `CREATED` | início / cliente autenticado | pedido elegível e tentativa idempotente | registra tentativa | mesma chave retorna tentativa |
| `CREATED` | `REDIRECT_READY` | resposta do provedor / backend | checkout hospedado criado | entrega URL sem dados de cartão | repetição retorna URL válida |
| `REDIRECT_READY` | `PENDING` | provedor/webhook/reconciliação | referência reconhecida | aguarda decisão | no-op |
| `PENDING` ou `UNKNOWN` | `APPROVED` | webhook/reconciliação / provedor validado | assinatura, pedido, valor e moeda válidos | tenta converter reserva | repetição no-op; aprovação tardia segue regra própria |
| `PENDING` ou `UNKNOWN` | `DECLINED` | provedor validado | evento compatível | libera reserva conforme pedido | no-op |
| `PENDING` | `EXPIRED` | timeout/expiração validada | prazo vencido sem aprovação | libera reserva conforme pedido | no-op |
| `CREATED` ou `PENDING` | `CANCELED` | cancelamento pelo provedor/admin | operação permitida | libera reserva | no-op |
| qualquer | `UNKNOWN` | timeout técnico / backend | resultado não verificável | não cria nova cobrança automaticamente | atualização posterior reconcilia |

Um evento atrasado aprovado depois de `EXPIRED` pode levar a `APPROVED` com marca de aprovação tardia e mover o pedido para `CONFIRMED` ou `PAYMENT_REVIEW`. Evento inválido é armazenado como rejeitado e nunca muda o estado.

### 5.3 Entrega

| Origem | Destino | Evento/quem | Pré-condições | Estoque | Repetição |
|---|---|---|---|---|---|
| `NOT_STARTED` | `SEPARATING` | iniciar separação / admin | pedido `CONFIRMED` | já baixado na confirmação | no-op |
| `SEPARATING` | `SHIPPED` | registrar envio / admin | remessa válida; rastreio quando exigido | nenhuma alteração | mesmo evento no-op |
| `SHIPPED` | `DELIVERED` | registrar entrega / admin | envio existente | nenhuma alteração | no-op |
| `SHIPPED` ou `DELIVERED` | `RETURN_REQUESTED` | abrir devolução / cliente ou admin | itens e motivo registrados | nenhuma alteração | retorna pedido de devolução |
| `RETURN_REQUESTED` | `RETURN_RECEIVED` | receber e inspecionar / admin | recebimento confirmado | ainda nenhuma; decisão vem depois | no-op |
| `SEPARATING` | `CANCELED` | cancelamento / admin | pedido cancelável e decisão física registrada | só movimenta após confirmação física | conflito retorna 409 |

## 6. Autenticação e segurança

### Comparação e recomendação

| Opção | Vantagem | Custo/risco |
|---|---|---|
| Sessão opaca no servidor | logout e revogação diretos, sessão pequena, fácil isolamento por usuário | exige tabela de sessão e compartilhamento de sessão se houver múltiplas instâncias |
| JWT apenas no navegador | fácil de distribuir sem consulta a sessão | revogação e logout são difíceis; risco maior de armazenamento e vazamento; não traz benefício necessário ao monólito |
| JWT curto com refresh rotativo | útil para múltiplos consumidores independentes | mais estados, rotação, revogação e cuidado com armazenamento; complexidade desnecessária agora |

Recomendação: sessão opaca aleatória, armazenada em cookie `HttpOnly`, `Secure`, com expiração absoluta e ociosidade, token armazenado somente como hash, rotação após login e revogação efetiva no logout. A senha usa hash adaptativo escolhido e configurado na Fase 1. A API restringe por padrão e autoriza `CUSTOMER`/`ADMIN` por caso de uso.

Topologia assumida: frontend Next.js e API sob o mesmo domínio registrável, por exemplo `www.exemplo` e `api.exemplo`, com HTTPS e origens explicitamente configuradas. Mesmo sendo same-site, métodos que alteram estado exigem proteção CSRF por token sincronizado ou double-submit e `Origin`/`Referer` verificados quando aplicável. CORS aceita somente origens configuradas, `credentials` apenas onde necessário e nenhum wildcard com credenciais. Se a topologia mudar para domínios registráveis diferentes, devem ser revistos `SameSite`, CORS, CSRF, cookies, proxy e política de confiança.

Recuperação usa token aleatório de uso único, armazenado como hash, expirado rapidamente, invalidado ao usar e sem aparecer em logs. Respostas públicas não confirmam se o e-mail existe. Login, recuperação, webhook e endpoints sensíveis têm limitação de tentativas e auditoria sem segredos.

O primeiro administrador é criado por procedimento controlado de ambiente/migration/CLI com credencial fornecida fora do código, ou por convite administrativo único. Cadastro público nunca aceita `role`. Logs não incluem senha, cookie, token, URL completa de recuperação, cartão, payload sensível ou segredo de provedor; usam identificador de correlação.
## 7. Materialização da identidade na Fase 3

A implementação da Fase 3 materializa `app_users` e `password_reset_tokens` na migration `V2__create_identity_and_session_tables.sql`. O e-mail normalizado é minúsculo com `Locale.ROOT`, preserva a forma apresentada ao cliente e possui unicidade no banco. A senha fica somente como hash BCrypt; o cadastro público sempre atribui `CUSTOMER`.

As sessões usam Spring Session JDBC, com as tabelas oficiais `spring_session` e `spring_session_attributes` no schema `perfumestore`, criadas e versionadas pelo Flyway. O cookie opaco `SESSION` é `HttpOnly`, usa `SameSite` configurável e é `Secure` em produção. O contexto de segurança é persistido pelo repositório JDBC; o logout remove a sessão e a redefinição de senha remove todas as sessões cujo principal é o UUID do usuário. Não há JWT, Redis ou tabela proprietária paralela de sessão.

O schema das tabelas de Spring Session não é inicializado automaticamente pela biblioteca: `spring.session.jdbc.initialize-schema=never` e a migration V2 são a única fonte de evolução. A expiração por inatividade é configurável, com padrão local de 30 minutos, e a limpeza é executada por cron do repositório JDBC.

## 7. Implementação do estoque na Fase 5

A migration `V4__create_inventory_tables.sql` cria `inventory_balances`, `stock_reservations` e `inventory_movements`. O saldo é uma linha única por apresentação, com `integer` não negativo, `reserved_quantity <= physical_quantity` e `version` técnico. Linhas são criadas sob demanda com zero; a leitura pública de uma apresentação sem linha retorna disponibilidade zero.

`inventory_movements` é append-only e registra deltas, saldos anterior/posterior, motivo, chave da operação, reserva, ator quando houver, correlação e instante. A chave operacional é única. Movimentações internas de reserva, confirmação e liberação não inventam um administrador humano; o ator é nulo quando a operação futura vier de pedido/checkout.

`InventoryService` bloqueia a linha de saldo com `SELECT ... FOR UPDATE` dentro da transação. Reserva, confirmação, liberação e entrada/ajuste gravam saldo, reserva e histórico na mesma transação. A unicidade das chaves e as constraints do PostgreSQL permanecem como defesa final. Como a Fase 5 opera uma apresentação por chamada, a ordem de lock é única; o futuro checkout deverá ordenar locks por `presentation_id` ao reservar múltiplas linhas.

O serviço expõe portas internas para reservar, confirmar e liberar, sem endpoint público ou endpoint administrativo de teste. A expiração automática não foi iniciada nesta fase e será definida com o ciclo de pedidos.

## 8. Endereços e frete na Fase 6

O módulo address usa AddressController, AddressService e AddressRepository. addresses possui UUID estável, FK para app_users, campos civis do destino, version, timestamps e deleted_at. O índice por proprietário, remoção e atualização sustenta a listagem privada. A propriedade é verificada na consulta SQL e no serviço; o DTO não recebe userId.

O módulo shipping usa ShippingProperties, ShippingService e ShippingQuoteRepository. A regra de atendimento é configuração externa de faixas de CEP, validada contra formatos, inversões e sobreposições. shipping_quotes guarda apenas o snapshot necessário do destino, modalidade, valor numeric(12,2) em BRL, prazo, versão e validade. A persistência não representa pedido nem reserva.

A V5 adiciona as duas tabelas e índices sem alterar V1–V4. A FK de endereços usa ON DELETE RESTRICT; a remoção normal é lógica. Cotações não são expostas no catálogo nem em respostas de outros clientes. O futuro checkout deve revalidar a cotação e criar o snapshot comercial do pedido.

## Fase 7 - modulo cart e V6

O modulo `cart` depende de `auth`, `catalog` e do leitor de disponibilidade de `inventory`. Ele nao cria dependencia de pedido, pagamento ou reserva. `CartService` concentra a identidade visitante/autenticada, merge, calculo de subtotal, validacao de elegibilidade e limpeza; `CartRepository` encapsula JDBC e locks PostgreSQL.

V6 possui `carts` (proprietario por `user_id` ou `visitor_token_hash`), `cart_items` (uma linha por `cart_id` e `presentation_id`) e `cart_mutation_keys` para idempotencia de inclusoes. Chaves estrangeiras preservam apresentacoes e contas; itens dependem do carrinho. Estados `ACTIVE`, `EXPIRED` e `TRANSFERRED` impedem reuso de token transferido.

As operacoes que modificam o mesmo carrinho bloqueiam a linha do carrinho no PostgreSQL. A unicidade do banco impede duas linhas para a mesma apresentacao; `version` e atualizacao condicional impedem sobrescrita silenciosa. O merge ordena os UUIDs dos dois carrinhos antes de obter locks, reduzindo risco de deadlock em logins concorrentes. O scheduler de limpeza usa lotes e TTL configuravel.

A resposta do carrinho e DTO, com item, preco vigente, subtotal, disponibilidade informativa e avisos. Nenhum token, hash, sessao, dado administrativo, saldo fisico ou reservado e exposto.
## Fase 8 — pedido e checkout

A migration V7 cria `orders`, `order_items`, `order_address_snapshots`, `order_shipping_snapshots`, `order_reservations` e `order_status_history`. O pedido mantém estados comerciais, financeiros e logísticos separados, além de número público, fingerprint da requisição de idempotência, prazo de pagamento e valores históricos.

O `OrderService` é a fronteira transacional entre carrinho, catálogo, frete e inventário. A criação usa o carrinho autenticado e limpa seus itens somente após persistir snapshots e reservar todas as apresentações. Falhas fazem rollback do pedido, snapshots, reservas e movimentações. A chave única `(user_id, idempotency_key)` é protegida pelo PostgreSQL; uma linha inicial de pedido funciona como lock transacional para reenvios concorrentes.

Pedidos não pagos vencidos são processados em lotes com `FOR UPDATE SKIP LOCKED`, com reservas liberadas em ordem determinística. A aprovação simulada confirma reservas; aprovação após expiração leva o pedido a `PAYMENT_REVIEW` sem recriar estoque.
## Fase 9 - infraestrutura de pagamentos MOCK

O domínio `payment` mantém a interação com um provedor atrás de `PaymentProvider`. `MockPaymentProvider` é carregado somente em `local` e `test`; em outros perfis existe apenas um adaptador indisponível e `PAYMENT_ENABLED` permanece falso por padrão.

A migration V8 cria `payment_attempts`, `payment_attempt_history`, `payment_events`, `payment_reconciliations` e `mock_payment_states`. Tentativas guardam o valor e a moeda do pedido, a chave de idempotência, o fingerprint da operação, referências externas e a expiração. Eventos possuem unicidade por provedor e identificador externo, hash do corpo, autenticidade e estado de processamento. O estado MOCK é persistido para permitir reconciliação após perda de notificação e reinicialização.

O recebimento de evento segue: validar HMAC e janela temporal, persistir o evento idempotentemente, bloquear a tentativa, comparar pedido/tentativa/valor/moeda, aplicar a transição de pagamento e delegar os efeitos comerciais ao `OrderService` e ao `InventoryService`. A confirmação de estoque nunca é executada diretamente por controller ou repositório de pagamento.

Eventos concorrentes utilizam unicidade no PostgreSQL e `FOR UPDATE` na tentativa e no pedido. A confirmação de uma reserva é idempotente. Aprovação após a liberação de uma reserva mantém o pagamento aprovado, mas move o pedido para `PAYMENT_REVIEW`; não recria reserva, não baixa estoque e não autoriza entrega.

Estados de tentativa persistidos: `PENDING`, `APPROVED`, `REJECTED`, `FAILED`, `CANCELLED`, `EXPIRED`, `UNKNOWN` e `REVIEW`. Eventos antigos não regridem estados terminais. Reconciliação consulta o adaptador, reutiliza o mesmo processamento idempotente e registra seu resultado.

O simulador legado da Fase 8 é apenas uma fachada para esse serviço central. Não existem cobranças reais, dados de cartão ou fallback MOCK em produção.
## Fase 10 — operação administrativa e pós-venda

A migration V9 adiciona logística do pedido, auditoria administrativa, reembolsos e devoluções. O pedido mantém estados comerciais, financeiros e logísticos separados. A separação exige pedido confirmado, pagamento aprovado, reservas confirmadas e ausência de revisão; o envio exige separação; a entrega exige envio.

Cancelamento, reembolso e devolução são processos distintos. Cancelamento de pedido não confirma reembolso nem reposição física. Reposição só ocorre após recebimento e inspeção, usa `InventoryService` com movimentação `RESTOCK` e possui chave idempotente. O histórico administrativo é append-only e registra ator, operação, estados, motivo e instante.
