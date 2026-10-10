# PerfumeStore

Base do backend do PerfumeStore. Esta entrega implementa as Fases 1, 2 e 3 do roadmap: aplicação Spring Boot executável, PostgreSQL local, Flyway, padrões de API, validação, erros, paginação, OpenAPI, segurança, autenticação por sessão e recuperação de acesso.

## Versões fixadas

- Java: 17, requisito mínimo confirmado para Spring Boot 4.1.1.
- Spring Boot: 4.1.1, release estável consultada em 10/10/2026.
- Maven Wrapper: 3.3.4.
- Maven distribuído pelo wrapper: 3.9.16.
- PostgreSQL local e Testcontainers: imagem `postgres:18.6`.
- Testcontainers for Java: 2.0.5, fixado por BOM para os módulos de teste.
- Flyway: versão gerenciada pelo parent do Spring Boot 4.1.1, com `spring-boot-starter-flyway` e `flyway-database-postgresql`.
- Driver PostgreSQL: versão gerenciada pelo parent do Spring Boot; a página oficial do pgJDBC consultada informa a série 42.7.14 para Java 8 ou superior.

As fontes oficiais usadas para a escolha foram [Spring Boot — projeto](https://spring.io/projects/spring-boot), [requisitos do Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html), [Maven — histórico de releases](https://maven.apache.org/docs/history), [Maven Wrapper](https://maven.apache.org/tools/mavenwrapper.html), [Flyway — PostgreSQL](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database), [pgJDBC](https://jdbc.postgresql.org/download/) e [imagem oficial do PostgreSQL](https://hub.docker.com/_/postgres).

O Spring Boot exige Java 17 ou superior e Maven 3.6.3 ou superior. O projeto usa Java 17 e fixa o Maven 3.9.16 no wrapper. A versão do Flyway é mantida pelo gerenciamento de dependências do Spring Boot para evitar sobrescrita sem necessidade.

## Estrutura

```text
Docs/
backend/
  .mvn/wrapper/
  src/main/java/com/perfumestore/backend/
    auth/ address/ cart/ catalog/ inventory/
    notification/ order/ payment/ shipping/ user/
  src/main/resources/
    db/migration/
  src/test/java/
  pom.xml
  mvnw
  mvnw.cmd
compose.yaml
.env.example
.github/workflows/backend-ci.yml
```

Os packages de domínio estão preparados, mas ainda não contêm controllers, entidades, repositories ou regras comerciais. Essas classes serão materializadas nas fases correspondentes.

Os componentes compartilhados de API já implementam o contrato de erros, correlação, paginação, ordenação permitida e serialização de dinheiro. Os controllers usados para exercitar esses contratos existem somente em `src/test/java` e não são publicados pela aplicação.

## Pré-requisitos

- JDK 17 disponível e `JAVA_HOME` apontando para ele.
- Docker Desktop com Docker Compose funcional.
- Git para versionamento.

No ambiente usado nesta etapa, `javac` 17 está em `C:\Program Files\Zulu\zulu-17`, enquanto `java` no PATH aponta para um runtime Java 8. No PowerShell, configure o JDK antes de chamar o wrapper:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Zulu\zulu-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
```

O Maven instalado no sistema não é necessário: `backend\mvnw.cmd` baixa e usa o Maven 3.9.16 definido no wrapper.

## Configuração local

Na raiz do projeto:

```powershell
Copy-Item .env.example .env
```

O Docker Compose lê `.env` automaticamente para interpolar `POSTGRES_*` e `POSTGRES_PORT`. O Spring Boot não carrega `.env` automaticamente. Para iniciar a aplicação, exporte as variáveis para o processo Java na mesma sessão do PowerShell:

```powershell
Get-Content .env | Where-Object { $_ -and -not $_.StartsWith('#') } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    Set-Item -Path "Env:$name" -Value $value
}

$env:SPRING_PROFILES_ACTIVE = 'local'
```

As variáveis `POSTGRES_*` configuram o container. As variáveis `SPRING_DATASOURCE_*` configuram o processo Java. A aplicação não usa fallback de banco ou senha no perfil de produção.

## PostgreSQL local

Inicie o serviço sem apagar o volume:

```powershell
docker compose up -d postgres
docker compose ps
```

O banco publica a porta configurada em `POSTGRES_PORT` somente no loopback. O volume nomeado `perfumestore-postgres-data` é persistente. Para parar sem remover dados:

```powershell
docker compose stop postgres
```

`docker compose down` remove o container e a rede, mas preserva o volume se `-v` não for usado. Não use `docker compose down -v` neste projeto sem uma decisão explícita de descarte.

## Aplicação, migration e saúde

Com o PostgreSQL saudável e as variáveis exportadas:

```powershell
Set-Location backend
.\mvnw.cmd --batch-mode verify
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

A aplicação usa `application.yml` como base e exige explicitamente o perfil `local`, `test` ou `prod`. O Flyway executa automaticamente no startup e valida as migrations. A primeira migration cria somente o schema lógico `perfumestore`; tabelas comerciais serão criadas nas fases de seus domínios.

Em outra janela:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Resposta esperada:

```json
{"status":"UP"}
```

Os endpoints públicos de infraestrutura são `/actuator/health`, `/v3/api-docs`, `/swagger-ui/index.html` e `/api/v1/security/csrf` nos perfis local e test. O Actuator expõe somente health e sem detalhes de componentes. A documentação OpenAPI fica desabilitada no perfil `prod`.

Para consultar a documentação local:

```powershell
Invoke-WebRequest http://localhost:8080/v3/api-docs
Start-Process http://localhost:8080/swagger-ui/index.html
```

O endpoint `GET /api/v1/security/csrf` devolve o nome do header e o token, além de emitir o cookie `XSRF-TOKEN`. O frontend deve usar `credentials: 'include'` e enviar o valor no header `X-XSRF-TOKEN` em operações inseguras. Esse token não é o cookie de sessão e a chamada técnica não autentica o usuário.

Para verificar o histórico do Flyway no banco local:

```powershell
Set-Location ..
docker compose exec postgres psql -U $env:POSTGRES_USER -d $env:POSTGRES_DB -c "select installed_rank, version, description, success from perfumestore.flyway_schema_history order by installed_rank;"
```

Uma segunda inicialização da aplicação deve validar a migration existente sem reaplicá-la.

## Testes e build

O `verify` usa o Maven Wrapper. `InfrastructureIntegrationTest` verifica PostgreSQL, Flyway e health; `ApiContractIntegrationTest` verifica os contratos HTTP, segurança, CORS, CSRF e OpenAPI usando PostgreSQL real via Testcontainers. Os testes não usam H2 nem conectam ao banco do Compose.

O artefato executável é gerado em `backend\target\perfumestore-backend-0.0.1-SNAPSHOT.jar`:

```powershell
Set-Location backend
.\mvnw.cmd --batch-mode clean verify
java -jar target\perfumestore-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## Solução de problemas

- **Java 8 aparece no PATH:** defina `JAVA_HOME` para um JDK 17 e coloque `$env:JAVA_HOME\bin` no início de `Path` na sessão atual.
- **Docker indisponível:** confirme `docker info` e o Docker Desktop; o teste de integração depende do daemon.
- **Porta 54329 ocupada:** altere `POSTGRES_PORT` no `.env` e também `SPRING_DATASOURCE_URL` para a mesma porta.
- **Porta 8080 ocupada:** altere `SERVER_PORT` no `.env` e consulte o health check na nova porta.
- **Perfil incorreto:** use `-Dspring-boot.run.profiles=local` no plugin ou `--spring.profiles.active=local` no jar. Produção exige todas as variáveis `SPRING_DATASOURCE_*` explicitamente.
- **Migration inconsistente:** interrompa a aplicação e investigue o histórico; não use `flyway clean`, reparo automático ou remoção do volume para esconder divergências.

## Integração contínua

`.github/workflows/backend-ci.yml` executa em push e pull request, usa JDK 17, Maven Wrapper, cache do Maven e `verify`. Os testes criam PostgreSQL isolado por Testcontainers. A existência do workflow foi validada localmente; sua execução no GitHub ainda depende de um push ou pull request.

## Convenções da API na Fase 2

- Erros usam `application/problem+json`, código estável, mensagem em português, `correlationId` e `fieldErrors` quando aplicável.
- `X-Correlation-Id` recebido é aceito somente com caracteres seguros e até 64 caracteres; caso contrário, o servidor gera um UUID. O valor aparece no header de resposta e no contexto dos logs.
- Paginação começa em `page=0`, usa `size=20` por padrão e aceita no máximo `100`. Ordenação usa `sort=campo,asc|desc`; cada endpoint futuro deverá fornecer sua própria lista de campos permitidos e o servidor acrescentará desempate estável.
- `BigDecimal` comercial é serializado como string decimal em `BRL`, com escala 2 quando o DTO exigir. Instantes usam ISO-8601 em UTC; datas civis usam `YYYY-MM-DD`.
- Todas as rotas, exceto as explicitamente públicas, exigem autenticação. Não há login, usuários, sessão persistida ou endpoints comerciais nesta fase; não existe senha gerada de desenvolvimento.
- CORS usa origens explícitas da variável `CORS_ALLOWED_ORIGINS`. Em produção, a variável é obrigatória e não há origem de desenvolvimento como fallback.

Os contratos comerciais continuam especificados em `Docs/backend/02-contratos-api.md`; somente os endpoints técnicos efetivamente implementados nesta fase são publicados.

## Limites desta fase

Ainda não existem cadastro, login, autenticação persistida, recuperação de senha, usuários, catálogo, estoque, carrinho, frete, pedidos, pagamentos, notificações comerciais ou endpoints de negócio. A aplicação tem apenas a base de segurança e o endpoint técnico de CSRF. Os roadmaps e a especificação da Fase 0 continuam sendo a referência para as próximas fases.
## Fase 3 — autenticação e recuperação

A base implementada inclui cadastro de clientes, login/logout, sessões opacas persistidas no PostgreSQL, consulta/atualização limitada do perfil, recuperação de senha e provisionamento controlado do primeiro administrador. Os endpoints comerciais de catálogo, carrinho, pedido e pagamento continuam fora desta fase.

### Mailpit local

Com as variáveis do `.env` carregadas para o processo Docker, inicie o PostgreSQL e o capturador SMTP sem remover volumes:

```powershell
docker compose up -d postgres mailpit
Start-Process http://localhost:8025
```

O Mailpit usa SMTP em `localhost:1025` e interface em `http://localhost:8025`, somente para desenvolvimento. Nenhuma mensagem é enviada a destinatários reais.

### Sessão e CSRF

O cliente chama `GET /api/v1/security/csrf`, guarda o cookie `XSRF-TOKEN` e envia seu valor no header `X-XSRF-TOKEN` em `POST`, `PATCH` e demais métodos que alteram estado, sempre com `credentials: include`. A sessão autenticada usa o cookie HttpOnly `SESSION`; seu valor não deve ser lido pelo JavaScript nem aparecer em logs. O login gera/rotaciona a sessão e o logout a revoga. Após logout ou redefinição de senha, o cliente deve obter novo CSRF antes de uma operação seguinte.

### Executar autenticação local

Carregue `.env` para a sessão do PowerShell e mantenha `SPRING_PROFILES_ACTIVE=local`. Depois do banco/Mailpit:

```powershell
Set-Location backend
.\mvnw.cmd --batch-mode clean verify
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

O fluxo manual usa os endpoints `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `GET/PATCH /api/v1/auth/me`, `POST /api/v1/auth/logout`, `POST /api/v1/auth/password-recovery-requests` e `POST /api/v1/auth/password-resets`. A resposta de recuperação é genérica e o token só pode ser obtido no e-mail capturado pelo Mailpit.

### Provisionar o primeiro ADMIN

O provisionamento não possui endpoint público e não ocorre em startup normal. Forneça as quatro variáveis `ADMIN_PROVISIONING_*` apenas na sessão atual do processo e execute o perfil dedicado:

```powershell
$env:ADMIN_PROVISIONING_ENABLED = 'true'
$env:ADMIN_PROVISIONING_NAME = '<nome fornecido fora do repositório>'
$env:ADMIN_PROVISIONING_EMAIL = '<e-mail fornecido fora do repositório>'
$env:ADMIN_PROVISIONING_PASSWORD = '<senha fornecida fora do repositório>'
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local,provision-admin
Remove-Item Env:ADMIN_PROVISIONING_ENABLED,Env:ADMIN_PROVISIONING_NAME,Env:ADMIN_PROVISIONING_EMAIL,Env:ADMIN_PROVISIONING_PASSWORD
```

O processo dedicado não abre servidor HTTP. A operação é idempotente para o mesmo administrador, não promove um `CUSTOMER` e não redefine a senha de administrador existente. Nunca coloque valores reais no `.env.example`, no histórico do shell ou em logs.

### Parâmetros e limites da Fase 3

O timeout de inatividade da sessão e o TTL do token de recuperação têm padrão de 30 minutos. Os limites padrão são login 5 por minuto, recuperação 5 por 15 minutos, redefinição 5 por 15 minutos e cadastro 10 por 10 minutos. O limitador é local à instância; múltiplas réplicas exigirão mecanismo compartilhado antes de produção.

O provedor de e-mail real, MFA, verificação de e-mail, troca de e-mail/senha, frontend, catálogo, pedidos, pagamentos e deploy não foram implementados. O workflow CI existe, mas seu resultado no GitHub depende de execução por push ou pull request.
