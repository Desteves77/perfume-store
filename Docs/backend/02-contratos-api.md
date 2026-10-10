# PerfumeStore — Fase 0: contratos iniciais da API

Status: especificação inicial, sujeita a ajuste controlado durante a implementação. Base URL ilustrativa: `/api/v1`. Os endpoints comerciais abaixo continuam planejados; a Fase 2 implementa somente os contratos compartilhados e os endpoints técnicos indicados na seção seguinte.

## 0. O que está implementado na Fase 2

Estão disponíveis nos perfis `local` e `test`:

- `GET /actuator/health`: health público, sem detalhes internos.
- `GET /v3/api-docs` e `GET /swagger-ui/index.html`: documentação OpenAPI técnica; ambos ficam desabilitados no perfil `prod`.
- `GET /api/v1/security/csrf`: emite `XSRF-TOKEN` e devolve `{ "headerName": "X-XSRF-TOKEN", "token": "..." }`. É um endpoint técnico e não implementa login.

Os controllers de fixture usados para validar os contratos ficam exclusivamente em `src/test/java` e não fazem parte do artefato publicado. Cadastro, sessão persistida, catálogo, carrinho e demais endpoints comerciais ainda não estão implementados.

## 1. Convenções

- JSON em UTF-8; datas em ISO-8601 UTC, por exemplo `2026-10-10T12:00:00Z`.
- Valores monetários são strings decimais com duas casas e moeda explícita: `"149.90"`, `"BRL"`.
- `page` começa em 0; `size` padrão sugerido 20 e máximo 100; o servidor limita e valida ordenação.
- Recursos públicos retornam somente catálogo ativo, disponibilidade de apresentação e dados de imagem aprovados.
- Recursos privados usam sessão por cookie e `Cache-Control: no-store`.
- Toda resposta inclui `X-Correlation-Id` ou o valor equivalente devolvido no corpo de erro.
- Operações comerciais de criação e pagamentos recebem `Idempotency-Key` quando indicado.
- Códigos de estado são estáveis para o frontend; mensagens podem ser aprimoradas sem trocar o código.

## 2. Formato de erro

```json
{
  "type": "https://api.perfumestore.local/problems/validation",
  "title": "Dados inválidos",
  "status": 422,
  "code": "VALIDATION_ERROR",
  "message": "Corrija os campos informados.",
  "correlationId": "4c4e8a55-66bf-4a0a-a1a3-2b2d9f671201",
  "fieldErrors": [
    {
      "field": "email",
      "code": "INVALID_FORMAT",
      "message": "Informe um e-mail válido."
    }
  ]
}
```

`fieldErrors` é omitido ou vazio quando não se aplica. Nunca há stack trace, SQL, nome de classe, segredo, token ou decisão interna no corpo. Exemplos de códigos: `AUTHENTICATION_REQUIRED`, `FORBIDDEN`, `RESOURCE_NOT_FOUND`, `PRODUCT_INACTIVE`, `OUT_OF_STOCK`, `CHECKOUT_REVIEW_REQUIRED`, `IDEMPOTENCY_KEY_REUSED`, `PAYMENT_PENDING`, `PAYMENT_REVIEW_REQUIRED`, `INVALID_WEBHOOK`, `SHIPPING_DESTINATION_UNSERVED`.

Na infraestrutura implementada, o mapeamento é: `400` para sintaxe, tipos e parâmetros inválidos; `401` para ausência de autenticação; `403` para autorização, CSRF ou origem CORS rejeitada; `404` para recurso inexistente; `405` para método não permitido; `415` para mídia não suportada; `422` para validação semântica; `500` para falha inesperada. Respostas privadas e erros usam `Cache-Control: no-store`.

O `X-Correlation-Id` é validado (1 a 64 caracteres alfanuméricos, `-`, `_`, `.` ou `:`) ou gerado pelo servidor. O mesmo valor aparece no header de resposta, no erro e no MDC durante a requisição. Corpos, cookies, tokens e headers de autorização não são registrados automaticamente.

## 2.1. Paginação, ordenação e serialização

- `page` começa em `0`, o padrão de `size` é `20` e o máximo é `100`. Valores negativos ou zero para `size`, e valores acima do máximo, produzem `VALIDATION_ERROR` com erro de campo.
- `sort` usa exatamente `campo,asc` ou `campo,desc`. A lista de campos válidos é definida por cada endpoint; nomes desconhecidos produzem `INVALID_SORT`. A resposta informa `page`, `size`, `totalElements`, `totalPages`, `items` e a ordenação aplicada. O servidor acrescenta `id,asc` como desempate quando necessário.
- Valores monetários são `BigDecimal` e aparecem como strings decimais, com moeda explícita `BRL`; DTOs comerciais devem definir a escala e o arredondamento exigidos pelo domínio.
- Instantes técnicos são ISO-8601 UTC, por exemplo `2026-10-10T12:00:00Z`. Datas civis são strings `YYYY-MM-DD`, sem conversão de fuso.
- Os contratos de dinheiro, data e paginação são compartilhados; cálculos de preço, frete e totais continuam exclusivamente no backend dos módulos comerciais.

## 3. Endpoints públicos e de autenticação

| Método e caminho | Acesso | Entrada principal / saída | Status e erros | Idempotência |
|---|---|---|---|---|
| `GET /catalog/categories` | público | filtros opcionais; categorias ativas | `200` | não |
| `GET /catalog/products` | público | `q`, `category`, `page`, `size`, `sort`; página de resumos | `200`, `400` | não |
| `GET /catalog/products/{slug}` | público | slug; produto, imagens, apresentações e disponibilidade | `200`, `404` | não |
| `POST /auth/register` | público | nome, e-mail, senha; usuário sem papel administrativo | `201`, `409`, `422` | chave opcional para retry seguro, se implementada |
| `POST /auth/login` | público | e-mail e senha; sessão e resumo do usuário | `200`, `401`, `429`, `409` em conflito de carrinho | não; mesma tentativa não deve ser repetida automaticamente |
| `POST /auth/logout` | cliente | sem corpo; revoga sessão | `204` | sim, no-op se já revogada |
| `GET /auth/me` | cliente | usuário autenticado e ações gerais | `200`, `401` | não |
| `GET /auth/csrf` | público/cliente | token CSRF para métodos inseguros | `200` | não |
| `POST /auth/password-recovery-requests` | público | e-mail; resposta genérica | `202`, `422`, `429` | pode aceitar chave técnica; resposta não revela existência |
| `POST /auth/password-resets` | público | token, nova senha; invalida tokens/sessões conforme política | `204`, `400`, `410`, `422` | mesma chave pode devolver o mesmo resultado; token é de uso único |

## 4. Endereços, carrinho e frete

| Método e caminho | Acesso | Entrada principal / saída | Status e erros | Idempotência |
|---|---|---|---|---|
| `GET /me/addresses` | cliente | lista do próprio cliente | `200`, `401` | não |
| `POST /me/addresses` | cliente | endereço; endereço criado | `201`, `422` | recomendado por chave de cliente |
| `PATCH /me/addresses/{id}` | cliente | campos permitidos; endereço atualizado | `200`, `404`, `409`, `422` | por versão ou chave |
| `DELETE /me/addresses/{id}` | cliente | remoção lógica | `204`, `404`, `409` | sim |
| `GET /cart` | público/cliente | cookie identifica carrinho; itens, subtotal e problemas | `200` | não |
| `POST /cart/items` | público/cliente | `presentationId`, `quantity`; carrinho recalculado | `200`, `404`, `409`, `422` | opcional; retry deve usar chave para não somar duas vezes |
| `PATCH /cart/items/{presentationId}` | público/cliente | nova quantidade explícita | `200`, `404`, `409`, `422` | recomendado |
| `DELETE /cart/items/{presentationId}` | público/cliente | remove linha escolhida | `204`, `404` | sim |
| `POST /cart/claim` | cliente | associa carrinho visitante autenticado | `200`, `409 CART_MERGE_REQUIRES_REVIEW` | sim |
| `POST /shipping/quotes` | cliente | `addressId`, referência ao carrinho; cotação | `200`, `409`, `422` | chave recomendada para evitar chamadas externas repetidas |

O servidor também pode chamar `claim` durante o login; se houver conflito, a resposta de login identifica a necessidade de resolver o carrinho sem criar pedido.

## 5. Pedidos e pagamentos

| Método e caminho | Acesso | Entrada principal / saída | Status e erros | Idempotência |
|---|---|---|---|---|
| `POST /orders` | cliente | `addressId`, `shippingQuoteId`; pedido, itens, totais e estados | `201`, `401`, `409`, `422` | `Idempotency-Key` obrigatório |
| `GET /me/orders` | cliente | filtros e paginação; apenas pedidos próprios | `200`, `401` | não |
| `GET /me/orders/{orderNumber}` | cliente | detalhe histórico, pagamento e entrega | `200`, `404`, `401` | não |
| `POST /me/orders/{orderNumber}/cancellation-requests` | cliente | motivo; solicitação | `202`, `404`, `409`, `422` | obrigatório |
| `POST /orders/{orderNumber}/payments` | cliente | tentativa; URL de checkout hospedado | `201`, `409`, `422` | obrigatório |
| `GET /me/orders/{orderNumber}/payment` | cliente | estado reconhecido, sem dados sensíveis | `200`, `404` | não |
| `POST /webhooks/payments/{provider}` | provedor, assinatura validada | evento do provedor; resposta técnica | `200`, `202`, `400`, `401`, `409` | `provider_event_id` obrigatório e único |

`POST /orders` devolve `201` na primeira criação e pode devolver `200` ou `201` conforme a política adotada quando a mesma chave reaparecer; o corpo deve ser idêntico ao resultado armazenado. O frontend deve consultar o pedido quando o transporte falhar antes de saber o resultado.

## 6. Administração

| Método e caminho | Acesso | Finalidade / saída | Status e erros | Idempotência |
|---|---|---|---|---|
| `GET /admin/categories` | admin | listar categorias | `200`, `401`, `403` | não |
| `POST /admin/categories` | admin | criar categoria | `201`, `403`, `409`, `422` | recomendada |
| `PATCH /admin/categories/{id}` | admin | editar/ativar/desativar | `200`, `404`, `409`, `422` | por versão |
| `GET /admin/products` | admin | listar produtos | `200`, `403` | não |
| `POST /admin/products` | admin | criar produto | `201`, `403`, `409`, `422` | recomendada |
| `GET/PATCH /admin/products/{id}` | admin | detalhe e edição de produto | `200`, `404`, `409`, `422` | por versão |
| `POST /admin/products/{id}/presentations` | admin | criar SKU, volume, preço e estado | `201`, `403`, `404`, `409`, `422` | recomendada |
| `PATCH /admin/presentations/{presentationId}` | admin | editar/ativar/desativar apresentação | `200`, `403`, `404`, `409`, `422` | por versão |
| `POST /admin/products/{id}/images` | admin | upload validado ou registro de imagem | `201`, `413`, `415`, `422` | recomendada |
| `PATCH/DELETE /admin/products/{id}/images/{imageId}` | admin | ordenar, marcar principal, desativar | `200`, `204`, `404`, `409` | sim para remoção |
| `POST /admin/inventory/adjustments` | admin | entrada/ajuste com apresentação, delta e motivo | `201`, `403`, `409`, `422` | obrigatório |
| `GET /admin/inventory/movements` | admin | histórico paginado | `200`, `403` | não |
| `GET /admin/orders` | admin | lista/filtros de pedidos | `200`, `403` | não |
| `GET /admin/orders/{orderNumber}` | admin | detalhe operacional | `200`, `403`, `404` | não |
| `POST /admin/orders/{orderNumber}/separation` | admin | inicia separação | `200`, `409`, `422` | sim |
| `POST /admin/orders/{orderNumber}/shipment` | admin | registra transportadora/rastreio e envio | `200`, `409`, `422` | obrigatório |
| `POST /admin/orders/{orderNumber}/delivery` | admin | registra entrega | `200`, `409` | sim |
| `POST /admin/orders/{orderNumber}/cancellations` | admin | decide cancelamento e reposição física | `200`, `409`, `422` | obrigatório |
| `POST /admin/orders/{orderNumber}/refunds` | admin | solicita reembolso | `202`, `403`, `404`, `409`, `422` | obrigatório |
| `POST /admin/refunds/{refundId}/reconciliation` | admin | registra consulta/evidência do provedor ou operação manual autorizada; só confirma quando o resultado verificado for compatível | `200`, `403`, `404`, `409`, `422` | obrigatório |
| `POST /admin/orders/{orderNumber}/returns` | admin | registra devolução/inspeção | `201`, `409`, `422` | obrigatório |
| `POST /admin/returns/{returnId}/inspection` | admin | registra inspeção e decisão de reposição física | `200`, `403`, `404`, `409`, `422` | obrigatório |
| `GET /admin/audit-logs` | admin autorizado | histórico de ações relevantes | `200`, `403` | não |

### Erros principais por grupo

| Grupo | Códigos esperados além de erro interno genérico |
|---|---|
| Catálogo público | `INVALID_QUERY`, `RESOURCE_NOT_FOUND`, `PAGE_SIZE_TOO_LARGE` |
| Cadastro/login/sessão | `VALIDATION_ERROR`, `EMAIL_ALREADY_REGISTERED`, `INVALID_CREDENTIALS`, `SESSION_EXPIRED`, `TOO_MANY_ATTEMPTS` |
| Endereços | `RESOURCE_NOT_FOUND`, `ADDRESS_NOT_OWNED`, `VALIDATION_ERROR`, `CONCURRENT_UPDATE` |
| Carrinho | `PRODUCT_INACTIVE`, `PRESENTATION_INACTIVE`, `QUANTITY_LIMIT_EXCEEDED`, `CART_MERGE_REQUIRES_REVIEW`, `CONCURRENT_UPDATE` |
| Frete | `SHIPPING_DESTINATION_UNSERVED`, `QUOTE_EXPIRED`, `QUOTE_DOES_NOT_MATCH_CART`, `VALIDATION_ERROR` |
| Pedido/checkout | `CHECKOUT_REVIEW_REQUIRED`, `OUT_OF_STOCK`, `IDEMPOTENCY_KEY_REUSED`, `ORDER_NOT_ACTIONABLE`, `RESOURCE_NOT_FOUND` |
| Pagamento/webhook | `PAYMENT_NOT_ACTIONABLE`, `PAYMENT_PENDING`, `PAYMENT_REVIEW_REQUIRED`, `INVALID_WEBHOOK`, `PAYMENT_AMOUNT_MISMATCH`, `PROVIDER_UNAVAILABLE` |
| Administração de catálogo/estoque | `FORBIDDEN`, `SKU_ALREADY_EXISTS`, `NEGATIVE_STOCK`, `INVALID_ADJUSTMENT`, `UPLOAD_TOO_LARGE`, `UNSUPPORTED_MEDIA_TYPE` |
| Envio/cancelamento/reembolso/devolução | `INVALID_STATE_TRANSITION`, `REFUND_ALREADY_REQUESTED`, `REFUND_NOT_CONFIRMED`, `REFUND_EVIDENCE_MISMATCH`, `RETURN_NOT_RECEIVED`, `RESTOCK_REQUIRES_INSPECTION` |

Uploads devem usar limite de tamanho, MIME permitido, verificação de conteúdo e armazenamento configurado; a URL de armazenamento não deve virar uma forma de contornar autorização.

## 7. Exemplos de fluxos críticos

### 7.1 Erro de validação por campo

```json
{
  "type": "https://api.perfumestore.local/problems/validation",
  "title": "Dados inválidos",
  "status": 422,
  "code": "VALIDATION_ERROR",
  "message": "Há campos que precisam de correção.",
  "correlationId": "9b8f1b6a-5b5b-4c98-8f88-817ac2fd0a03",
  "fieldErrors": [
    { "field": "quantity", "code": "MUST_BE_POSITIVE", "message": "Informe uma quantidade maior que zero." },
    { "field": "postalCode", "code": "INVALID_FORMAT", "message": "Informe um CEP válido." }
  ]
}
```

### 7.2 Produto e apresentações

```json
{
  "id": "b6e0c31c-96e9-4f5d-b0bf-2c9d99b313d1",
  "slug": "perfume-exemplo",
  "name": "Perfume Exemplo",
  "brand": "Marca Exemplo",
  "description": "Descrição aprovada do produto.",
  "category": { "id": "cat-001", "name": "Perfumes" },
  "images": [
    { "url": "https://cdn.exemplo/perfume.jpg", "alt": "Frasco do Perfume Exemplo", "isPrimary": true }
  ],
  "presentations": [
    { "id": "pres-50", "sku": "EXEMPLO-50", "volume": "50.00", "unit": "ML", "price": "199.90", "currency": "BRL", "active": true, "availability": { "available": true, "quantity": 3 } },
    { "id": "pres-100", "sku": "EXEMPLO-100", "volume": "100.00", "unit": "ML", "price": "299.90", "currency": "BRL", "active": true, "availability": { "available": false, "quantity": 0 } }
  ]
}
```

### 7.3 Consulta e alteração de carrinho

```json
{
  "id": "cart-123",
  "state": "ACTIVE",
  "items": [
    {
      "presentationId": "pres-50",
      "sku": "EXEMPLO-50",
      "name": "Perfume Exemplo 50 ml",
      "quantity": 2,
      "unitPrice": "199.90",
      "lineTotal": "399.80",
      "currency": "BRL",
      "availability": { "status": "AVAILABLE", "availableQuantity": 3 },
      "issues": []
    }
  ],
  "subtotal": "399.80",
  "currency": "BRL",
  "canCheckout": true,
  "issues": []
}
```

Alteração usa `PATCH /cart/items/pres-50` com `{ "quantity": 1 }` e retorna o carrinho inteiro recalculado. Em preço alterado, `issues` contém `PRICE_CHANGED`; em estoque insuficiente, a quantidade solicitada permanece e `canCheckout` torna-se `false`.

### 7.4 Cotação de frete

```json
{
  "quoteId": "quote-789",
  "serviceable": true,
  "method": "INITIAL_RULE",
  "amount": "24.90",
  "currency": "BRL",
  "estimatedDelivery": { "minDays": 3, "maxDays": 6 },
  "ruleVersion": "pending-commercial-definition",
  "expiresAt": "2026-10-10T12:30:00Z"
}
```

Destino não atendido retorna `422` ou `409` conforme a decisão de implementação, com `SHIPPING_DESTINATION_UNSERVED` e sem inventar valor ou prazo.

### 7.5 Criação de pedido

Requisição:

```http
POST /api/v1/orders
Idempotency-Key: 75eaf92a-7cbd-4c1c-8afc-9c31d60fc90a
Content-Type: application/json
```

```json
{
  "addressId": "addr-001",
  "shippingQuoteId": "quote-789"
}
```

Resposta:

```json
{
  "orderNumber": "PS-20261010-7K4M",
  "status": "AWAITING_PAYMENT",
  "paymentStatus": "NOT_STARTED",
  "deliveryStatus": "NOT_STARTED",
  "items": [
    { "sku": "EXEMPLO-50", "name": "Perfume Exemplo", "volume": "50.00", "unit": "ML", "quantity": 2, "unitPrice": "199.90", "lineTotal": "399.80", "currency": "BRL" }
  ],
  "subtotal": "399.80",
  "shipping": "24.90",
  "total": "424.70",
  "currency": "BRL",
  "reservationExpiresAt": "2026-10-10T12:45:00Z",
  "allowedActions": ["START_PAYMENT", "CANCEL"]
}
```

### 7.6 Consulta de pedido

```json
{
  "orderNumber": "PS-20261010-7K4M",
  "status": "CONFIRMED",
  "payment": { "status": "APPROVED", "provider": "configured-provider", "approvedAt": "2026-10-10T12:34:00Z" },
  "delivery": { "status": "NOT_STARTED", "method": "INITIAL_RULE", "estimatedDelivery": { "minDays": 3, "maxDays": 6 }, "trackingCode": null },
  "total": "424.70",
  "currency": "BRL",
  "items": [ { "sku": "EXEMPLO-50", "quantity": 2, "unitPrice": "199.90", "lineTotal": "399.80" } ],
  "shippingSnapshot": { "amount": "24.90", "postalCode": "00000000", "city": "Cidade", "state": "UF" },
  "allowedActions": ["REQUEST_CANCELLATION"]
}
```

### 7.7 Início de pagamento

```json
{
  "paymentAttemptId": "pay-321",
  "status": "REDIRECT_READY",
  "checkoutUrl": "https://provider.example/checkout/session",
  "expiresAt": "2026-10-10T12:45:00Z"
}
```

O frontend redireciona para `checkoutUrl`. Ao retornar, consulta o pedido; parâmetros do navegador não alteram `payment.status`.

### 7.8 Ação administrativa

Requisição `POST /admin/orders/PS-20261010-7K4M/shipment`:

```json
{
  "carrier": "transportadora-configurada",
  "trackingCode": "BR000000000",
  "shippedAt": "2026-10-11T14:00:00Z"
}
```

Resposta:

```json
{
  "orderNumber": "PS-20261010-7K4M",
  "status": "SHIPPED",
  "delivery": { "status": "SHIPPED", "carrier": "transportadora-configurada", "trackingCode": "BR000000000", "shippedAt": "2026-10-11T14:00:00Z" },
  "allowedActions": ["REGISTER_DELIVERY", "OPEN_RETURN"]
}
```

## 8. Integração segura com o frontend

O frontend deve enviar cookies de sessão e CSRF conforme a topologia, tratar `401`, `403`, `409`, `422` e `429` sem duplicar regras e preservar a chave de idempotência durante retries da mesma operação. Não deve fazer retry automático em `POST /orders` ou início de pagamento sem consultar o resultado. Campos como `allowedActions`, `canCheckout` e `issues` são informativos para a interface; a autorização final continua no backend.
## 9. Endpoints implementados na Fase 3

Os endpoints abaixo são a primeira parte executável dos contratos de autenticação. Eles usam a sessão opaca `SESSION`, continuam sujeitos ao CSRF para métodos que alteram estado e retornam `Cache-Control: no-store` em respostas privadas.

| Método e caminho | Acesso | Resultado principal | Status relevantes |
|---|---|---|---|
| `POST /api/v1/auth/register` | público | cria cliente; aceita `name`, `email`, `password`; o papel é atribuído pelo servidor | `201`, `409`, `422`, `429` |
| `POST /api/v1/auth/login` | público | autentica e grava o contexto em sessão JDBC; devolve usuário sem dados sensíveis | `200`, `401`, `429` |
| `POST /api/v1/auth/logout` | público/cliente | invalida sessão e tokens CSRF; repetir é seguro | `204`, `403` se CSRF for exigido e inválido |
| `GET /api/v1/auth/me` | cliente | consulta o usuário associado à sessão | `200`, `401` |
| `PATCH /api/v1/auth/me` | cliente | atualiza somente `name` | `200`, `401`, `422` |
| `POST /api/v1/auth/password-recovery-requests` | público | inicia recuperação com resposta genérica | `202`, `422`, `429` |
| `POST /api/v1/auth/password-resets` | público | consome token de uso único e exige novo login | `204`, `400`, `422`, `429` |

O cadastro não autentica automaticamente. O campo `role` não pertence ao DTO de entrada; mesmo que seja enviado, o servidor cria `CUSTOMER`. A recuperação sempre responde a mesma mensagem para e-mail inexistente, existente ou falha de envio. O token só aparece no e-mail local/serviço de notificação e nunca no corpo da API ou nos logs.

Exemplo de resposta de cadastro/login:

```json
{
  "id": "0f7c5c9a-3e1a-4a4a-9c02-3f5f4c7e5a11",
  "name": "Cliente",
  "email": "cliente@example.test",
  "role": "CUSTOMER",
  "createdAt": "2026-10-10T12:00:00Z"
}
```

Exemplo de recuperação:

```json
{
  "message": "Se o e-mail estiver cadastrado, enviaremos instruções para recuperar o acesso."
}
```

O provisionamento do primeiro ADMIN não possui endpoint HTTP. Ele é executado por processo dedicado, com perfil `provision-admin`, variáveis externas e `spring.main.web-application-type=none`.

Os endpoints de catálogo, endereços, carrinho, pedidos e administração comercial continuam planejados no contrato original e não são apresentados como implementados.
