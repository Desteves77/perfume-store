# Homologação integrada — Fase 12

Este documento define a matriz e o ambiente isolado usados para verificar o backend como conjunto. Os dados, pagamentos MOCK, frete sintético, Mailpit e certificado Caddy são exclusivos de homologação e não representam produção.

## Ambiente

- Compose: `compose.phase12-acceptance.yaml`.
- PostgreSQL isolado: porta `56600`, volume `perfumestore-phase12-acceptance-postgres-data`.
- Mailpit: SMTP `11425`, UI/API `18625`.
- HTTPS local: `https://localhost:18680`; HTTP `18080` redireciona para HTTPS.
- Imagem: `Dockerfile`, executada como usuário não-root.
- Migrations esperadas: V1–V10.

## Matriz

| Jornada | Módulos | Evidência | Risco |
| --- | --- | --- | --- |
| Cadastro/login/logout e recuperação | auth, user, notification | HTTP/HTTPS, sessão, Mailpit | enumeração, CSRF |
| Catálogo, imagem e preço | catalog | endpoints ADMIN/públicos | upload, publicação |
| Estoque e disponibilidade | inventory, catalog | saldo, histórico, concorrência | overselling |
| Carrinho anônimo e merge | cart, auth | cookie, sessão, subtotal | IDOR, perda de item |
| Endereço e frete | address, shipping | propriedade, quote, checkout | tarifa inválida |
| Checkout e pedido | order, cart, inventory | snapshots, idempotência | duplicação |
| Pagamento simulado | payment, order, inventory | webhook HMAC, reconciliação | evento fora de ordem |
| Outbox | notification, order, payment | Mailpit, retry, deduplicação | SMTP indisponível |
| Operação ADMIN | order, payment, inventory | prepare/ship/deliver/cancel | transição inválida |
| Pós-venda | payment, order, inventory | refund/return/inspect/restock | reposição dupla |
| Backup/restore | PostgreSQL, storage | dump e banco novo utilizável | recuperação parcial |

## Critérios de evidência

Cada jornada precisa registrar status HTTP, resposta sanitizada, estado persistido, correlação e, quando aplicável, mensagem no Mailpit. Health isolado não é evidência de integração comercial.

## Segurança integrada

O cliente usa a sessão para propriedade; nenhum `customerId` recebido do cliente é autoridade. CUSTOMER e anônimo são rejeitados nas rotas ADMIN. Escritas exigem CSRF. O perfil `prod` não registra controllers MOCK nem o adaptador MOCK; `401` isolado não é usado como prova dessa ausência.

## Limitações

O pagamento é exclusivamente MOCK, o frete é sintético, o Mailpit é local e o certificado é interno/autossinado. Não há provedor financeiro, SMTP ou regra comercial de frete real homologados. A garantia de entrega de e-mail não é exactly-once no SMTP.
