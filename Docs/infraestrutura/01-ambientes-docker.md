# Ambientes Docker do PerfumeStore

## Estrutura final

O caminho padrão é o Compose da raiz:

```powershell
Copy-Item .env.example .env
docker compose up -d
```

Esse comando inicia o ambiente local completo. Os scripts `scripts/dev-start.ps1`, `scripts/dev-stop.ps1` e `scripts/health-check.ps1` fornecem a mesma operação com validação explícita.

O ambiente de homologação integrada permanece separado:

```powershell
.\scripts\acceptance-start.ps1
.\scripts\health-check.ps1 -Environment acceptance
```

O template de produção está em `infra/compose/production.yaml`. Ele não é um deploy e não inclui serviços locais de desenvolvimento.

## Desenvolvimento local

| Serviço | Container/porta | Finalidade |
| --- | --- | --- |
| PostgreSQL | `127.0.0.1:54329` | Banco local persistente |
| Backend | `127.0.0.1:18081` | API Spring Boot |
| Frontend | `127.0.0.1:3100` | Loja Next.js |
| Mailpit SMTP | `127.0.0.1:1025` | Captura de e-mail local |
| Mailpit UI | `http://localhost:8025` | Consulta de mensagens locais |

O banco usa o volume `perfumestore_perfumestore-postgres-data`. As imagens locais usam `backend/data/catalog-images`, que permanece fora do Git. O ambiente local não habilita PaymentProvider MOCK por padrão e não representa produção. As portas 18081/3100 evitam conflito com processos locais e com o proxy HTTPS da homologação.

Para parar com preservação de dados:

```powershell
.\scripts\dev-stop.ps1
```

Esse script usa `docker compose stop`. Não usa `down -v`, `docker volume prune` ou `docker system prune`.

## Homologação integrada

O Compose histórico `compose.phase13-acceptance.yaml` continua sendo a configuração ativa de homologação por compatibilidade com os relatórios. O script de início usa `--force-recreate` para impedir que um container stale reutilize variáveis de outro ambiente, mas não remove volumes.

| Serviço | Porta | Volume principal | Situação |
| --- | --- | --- | --- |
| PostgreSQL | `127.0.0.1:56610` | `perfumestore-phase13-acceptance_perfumestore-phase13-postgres-data` | Homologação isolada |
| HTTPS Caddy | `https://localhost:19680` | volumes Caddy da Fase 13 | Homologação isolada |
| HTTP redirect | `http://localhost:19681` | — | Redireciona para HTTPS |
| Mailpit SMTP | `127.0.0.1:11435` | — | Captura sintética |
| Mailpit UI | `http://localhost:18635` | — | Consulta sintética |

O banco usa `perfumestore_phase13` e `phase13_user` definidos pelo arquivo de ambiente da Fase 13. O pagamento MOCK e o frete sintético são exclusivos deste ambiente.

## Produção

`infra/compose/production.yaml` contém apenas o serviço de aplicação e um volume de imagens. PostgreSQL, SMTP, domínio, proxy público, storage, provedor financeiro e segredos são externos e obrigatórios conforme o ambiente real.

O template não inclui Mailpit, PaymentProvider MOCK, credenciais sintéticas, frete sintético ou banco publicado. O perfil Spring `prod` mantém o pagamento desabilitado por padrão e não registra endpoints MOCK.

## Variáveis e precedência

- `.env` é carregado automaticamente pelo Compose da raiz.
- `--env-file` substitui o arquivo de ambiente usado para interpolação do Compose no comando informado.
- `environment` no YAML define o ambiente entregue ao container.
- O Spring Boot não lê `.env` automaticamente quando executado fora do Compose; nesse caso, exporte `SPRING_DATASOURCE_*` e demais variáveis na sessão do processo.
- As variáveis do banco devem ser iguais dentro do mesmo ambiente: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` e a URL/credenciais `SPRING_DATASOURCE_*`.

Nunca misture o `.env` local com `phase13-acceptance.env.example`. Não registre valores reais no Git.

## Containers, redes e volumes históricos

Os projetos Docker usam nomes separados: `perfumestore` para local, `perfumestore-phase12-acceptance` para a homologação backend e `perfumestore-phase13-acceptance` para a homologação integrada. Os Compose de fases anteriores e seus volumes permanecem preservados e classificados em `infra/legacy/README.md`.

Um container parado não é considerado descartável. A limpeza de containers, volumes, redes e backups históricos exige decisão posterior específica. Não executar comandos globais de prune.

## Troubleshooting PostgreSQL

O erro `28P01` da Fase 13 não foi causado por senha incorreta. O `docker inspect` mostrou que o `app` antigo usava `perfumestore_local/perfumestore`; o log do PostgreSQL informou que esse role não existia. A consulta read-only no volume comprovou `phase13_user|perfumestore_phase13`. A correção foi recriar somente o `app` com o arquivo `phase13-acceptance.env.example` e `--force-recreate`; o PostgreSQL e seu volume permaneceram intactos. O log posterior confirmou conexão, Flyway V10 e “No migration necessary”.

Para investigar um ambiente sem imprimir segredo:

```powershell
docker compose ps
docker inspect <container> --format '{{range .Config.Env}}{{println .}}{{end}}'
docker logs --tail 100 <container>
docker exec <postgres> pg_isready -U <usuario> -d <database>
```

Não use o valor de senha em comandos visíveis. Não altere role, senha ou volume antes de comprovar a causa e avaliar consumidores.

## Backup e limpeza futura

O procedimento de backup/restore da Fase 12 permanece em `scripts/phase12-backup.ps1`, `scripts/phase12-restore.ps1` e `Docs/backend/08-operacao-deploy-backup-recuperacao.md`. Backups e volumes históricos não são movidos ou apagados pela consolidação.

Uma limpeza futura deve listar explicitamente containers, redes, volumes e backups, confirmar que não estão em uso e receber autorização separada. `docker compose down -v`, `docker volume prune` e `docker system prune` não fazem parte dos scripts operacionais.
