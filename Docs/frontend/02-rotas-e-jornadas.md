# Frontend — rotas e jornadas

Rotas públicas: `/`, `/catalog`, `/catalog/[slug]`, `/contact`, `/shipping`, `/privacy` e `/terms`.

Conta: `/login`, `/register`, `/recover`, `/reset-password`, `/account`, `/account/addresses`, `/account/orders` e `/account/orders/[publicNumber]`.

Compra: `/cart` e `/checkout`. O login chama `/api/v1/cart/claim` para acionar o merge transacional previsto no backend; a criação do pedido usa uma chave estável por operação de tela.

Administração: `/admin`, `/admin/orders/[publicNumber]`, `/admin/catalog`, `/admin/inventory` e `/admin/notifications`. As rotas chamam somente endpoints existentes e não consultam o PostgreSQL diretamente.

Pagamentos MOCK são exibidos apenas como estado de homologação. O frontend não possui controles de aprovação financeira nem apresenta frete sintético como condição comercial real.
