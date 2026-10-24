# Compose por finalidade

O ambiente local padrão está em `compose.yaml` na raiz para que `docker compose up -d` seja o comando principal.

A homologação integrada continua usando `compose.phase13-acceptance.yaml` e `phase13-acceptance.env.example`. Esses nomes são mantidos para preservar a reprodução dos registros da Fase 13; os scripts `scripts/acceptance-start.ps1` e `scripts/acceptance-stop.ps1` encapsulam o uso correto.

`production.yaml` é somente um template operacional. Ele não inclui PostgreSQL local, Mailpit, Caddy de desenvolvimento, frete sintético nem PaymentProvider MOCK. Não executar em produção sem preencher as configurações externas e revisar a infraestrutura de hospedagem.

Os Compose `compose.catalog-acceptance.yaml`, `compose.phase6-acceptance.yaml`, `compose.phase7-acceptance.yaml`, `compose.phase10-acceptance.yaml`, `compose.phase11-acceptance.yaml` e `compose.phase12-acceptance.yaml` são históricos. Permanecem na raiz para reprodução das fases e não fazem parte do caminho padrão.
