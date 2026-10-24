# Operação, deploy, backup e recuperação

## Build e execução

1. Confirmar Java 17 e executar `backend\.\mvnw.cmd --batch-mode clean verify`.
2. Construir a imagem com `docker build -t perfumestore-backend:phase12 .`.
3. Fornecer datasource, SMTP, storage de imagens, CORS, cookies e segredos por ambiente; nunca por imagem ou Git.
4. Iniciar a aplicação com `SPRING_PROFILES_ACTIVE=prod`. Esse perfil mantém pagamento desabilitado até um provedor real ser configurado e não registra endpoints MOCK.
5. Conferir `/actuator/health/readiness` e depois `/actuator/health/liveness`. O actuator expõe somente health sem detalhes.

## Homologação HTTPS

Use `phase12-acceptance.env.example` somente com dados sintéticos. Execute `docker compose -p perfumestore-phase12-acceptance -f compose.phase12-acceptance.yaml --env-file phase12-acceptance.env.example up -d --build`. O Caddy termina HTTPS com certificado interno; `curl.exe -k` é apropriado somente para o teste local. PostgreSQL e Mailpit ficam publicados apenas em loopback.

## Migrations

Flyway executa V1–V10 em ordem, com `validate-on-migrate=true` e `clean-disabled=true`. Antes de uma mudança, gerar backup consistente, validar a imagem e consultar health. Não editar migrations aplicadas nem fazer rollback destrutivo automático; uma falha deve interromper a versão e ser tratada com correção incremental ou restauração isolada.

## Backup PostgreSQL

O script `scripts/phase12-backup.ps1` usa `pg_dump -Fc --no-owner` no PostgreSQL isolado e grava em diretório ignorado. Para recuperação completa, copiar também o volume/diretório de imagens na mesma janela de consistência; o dump sozinho não contém arquivos de imagem.

## Restauração comprovada

Use um nome novo com o prefixo `perfumestore_phase12_restore_` e `scripts/phase12-restore.ps1 -BackupPath <arquivo> -RestoreDatabase <nome-novo>`. O script nunca restaura sobre o banco de desenvolvimento. Após restaurar, apontar uma instância isolada da aplicação para o novo banco, verificar health, versão Flyway, pedidos, estoque, pagamentos, notificações e referências de imagens.

## Recuperação

- Falha da aplicação: reiniciar o container; sessões e dados permanecem no PostgreSQL.
- Falha SMTP: manter o pedido/pagamento confirmado; o outbox fica `RETRY_PENDING`/`FAILED` para diagnóstico.
- Falha de pagamento: não confirmar por retorno do navegador; reconciliar pelo fluxo do domínio.
- Falha de storage: restaurar banco e diretório de imagens do mesmo ponto de backup.
- Rollback de aplicação: voltar somente a imagem; validar compatibilidade de schema. Restaurar banco é uma decisão separada e exige destino isolado antes de qualquer operação.

## Limites de produção

Não usar MOCK, Mailpit, frete sintético ou credenciais do exemplo em produção. O certificado Caddy local não é certificado público. Ainda faltam provedor financeiro, SMTP transacional, storage de imagens, domínio/HTTPS público, regras comerciais de frete e frontend integrado.
