# Arquivos de ambiente

- Desenvolvimento: copie `.env.example` para `.env` na raiz. O `.env` real é ignorado pelo Git.
- Homologação: `phase13-acceptance.env.example` permanece na raiz para preservar os comandos da Fase 13 e é usado pelos scripts de homologação.
- Produção: `infra/env/production.env.example` é somente um template. Os valores reais devem ser fornecidos fora do Git por mecanismo de segredos.

O arquivo usado para interpolação do Compose não é automaticamente carregado pelo processo Java quando a aplicação é executada fora de um container. No Compose consolidado, o serviço `app` recebe explicitamente a URL interna `postgres:5432`, o usuário e a senha definidos para o mesmo ambiente.

Não misture `.env` local com `--env-file` de homologação. A opção `--env-file` deve ser informada no mesmo comando `docker compose` usado para o ambiente isolado.
