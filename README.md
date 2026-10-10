# PerfumeStore

Base do backend do PerfumeStore. Esta entrega implementa a Fase 1 do roadmap: aplicação Spring Boot executável, PostgreSQL local, Flyway, endpoint de saúde, teste de integração com PostgreSQL real via Testcontainers e build reproduzível pelo Maven Wrapper.

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

O único endpoint público de infraestrutura nesta etapa é `/actuator/health`. Detalhes de componentes, credenciais e configuração não são expostos.

Para verificar o histórico do Flyway no banco local:

```powershell
Set-Location ..
docker compose exec postgres psql -U $env:POSTGRES_USER -d $env:POSTGRES_DB -c "select installed_rank, version, description, success from perfumestore.flyway_schema_history order by installed_rank;"
```

Uma segunda inicialização da aplicação deve validar a migration existente sem reaplicá-la.

## Testes e build

O `verify` usa o Maven Wrapper. O teste `InfrastructureIntegrationTest` inicia um container PostgreSQL 18.6 com Testcontainers, sobe a aplicação em porta aleatória, verifica a migration e consulta `/actuator/health`. Ele não usa H2 e não conecta ao banco do Compose.

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

## Limites desta fase

Ainda não existem autenticação, Spring Security, usuários, catálogo, estoque, carrinho, frete, pedidos, pagamentos, notificações comerciais, OpenAPI ou endpoints de negócio. Os roadmaps e a especificação da Fase 0 continuam sendo a referência para as próximas fases.

