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

Os packages de domínio organizam a implementação por contexto. Auth e catálogo já possuem suas classes da fase atual; estoque, carrinho, pedidos, pagamentos e demais módulos serão materializados nas fases correspondentes.

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

## Limites históricos da Fase 2

Este trecho registra o estado anterior à implementação das Fases 3 e 4. Cadastro, login, autenticação persistida, recuperação de senha e catálogo foram implementados nas fases correspondentes; estoque, carrinho, frete, pedidos, pagamentos e notificações comerciais continuam fora do escopo atual. Os roadmaps e a especificação da Fase 0 continuam sendo a referência para as próximas fases.
## Fase 3 — autenticação e recuperação

A base implementada inclui cadastro de clientes, login/logout, sessões opacas persistidas no PostgreSQL, consulta/atualização limitada do perfil, recuperação de senha e provisionamento controlado do primeiro administrador. Os endpoints comerciais de catálogo, carrinho, pedido e pagamento continuam fora desta fase.

## Fase 4 — catálogo local

A Fase 4 adiciona categorias, produtos, apresentações e imagens. A publicação pública exige produto ativo, categoria ativa, ao menos uma apresentação ativa e exatamente uma imagem principal ativa. Desativar uma categoria oculta seus produtos sem alterar o estado persistido do produto.

Os endpoints públicos do catálogo são `GET /api/v1/catalog/categories`, `GET /api/v1/catalog/products`, `GET /api/v1/catalog/products/{slug}`, `GET /api/v1/catalog/products/{slug}/presentations`, `GET /api/v1/catalog/products/{slug}/images` e o conteúdo da imagem em `/content`. A administração fica sob `/api/v1/admin/**` e exige `ADMIN`, CSRF e sessão autenticada.

O armazenamento local usa `CATALOG_IMAGE_STORAGE_ROOT`, por padrão `backend/data/catalog-images`, fora do classpath e ignorado pelo Git. São aceitos PNG e JPEG decodificáveis, até 5 MiB, 8 imagens ativas por produto e 20 milhões de pixels. O servidor gera as chaves dos arquivos e não expõe caminhos físicos. Imagens removidas logicamente mantêm o metadado para recuperação; a limpeza física deve ser feita por rotina operacional futura, depois de reconciliar referências.

Na Fase 4, o catálogo ainda não informava estoque ou disponibilidade comprável. A Fase 5 adiciona essa informação por meio de `availability`; a migration `V3__create_catalog_tables.sql` continua sem tabelas de estoque, carrinho ou pedidos. Para executar os testes do catálogo com PostgreSQL real:

```powershell
Set-Location backend
.\mvnw.cmd --batch-mode -Dtest=CatalogIntegrationTest test
.\mvnw.cmd --batch-mode clean verify
```

O perfil `prod` exige `CATALOG_IMAGE_STORAGE_ROOT` e mantém OpenAPI desabilitado; o adaptador local é apropriado somente para desenvolvimento e testes. O armazenamento durável/CDN e a política de URLs de produção continuam pendentes.

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
$env:SERVER_PORT = '0'
$env:ADMIN_PROVISIONING_NAME = '<nome fornecido fora do repositório>'
$env:ADMIN_PROVISIONING_EMAIL = '<e-mail fornecido fora do repositório>'
$env:ADMIN_PROVISIONING_PASSWORD = '<senha fornecida fora do repositório>'
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local,provision-admin
Remove-Item Env:ADMIN_PROVISIONING_ENABLED,Env:ADMIN_PROVISIONING_NAME,Env:ADMIN_PROVISIONING_EMAIL,Env:ADMIN_PROVISIONING_PASSWORD,Env:SERVER_PORT
```

O processo dedicado não abre servidor HTTP. A operação é idempotente para o mesmo administrador, não promove um `CUSTOMER` e não redefine a senha de administrador existente. Nunca coloque valores reais no `.env.example`, no histórico do shell ou em logs.

### Parâmetros e limites da Fase 3

O timeout de inatividade da sessão e o TTL do token de recuperação têm padrão de 30 minutos. Os limites padrão são login 5 por minuto, recuperação 5 por 15 minutos, redefinição 5 por 15 minutos e cadastro 10 por 10 minutos. O limitador é local à instância; múltiplas réplicas exigirão mecanismo compartilhado antes de produção.

O provedor de e-mail real, MFA, verificação de e-mail, troca de e-mail/senha, frontend, estoque, pedidos, pagamentos e deploy não foram implementados. O workflow CI existe, mas seu resultado no GitHub depende de execução por push ou pull request.

### Homologação isolada do catálogo — Fase 4.1

A homologação local usa `compose.catalog-acceptance.yaml`, separado do `compose.yaml` de desenvolvimento. O ambiente possui PostgreSQL, volume, portas, credenciais sintéticas e diretório de imagens próprios. O arquivo `.env.catalog-acceptance` real é ignorado pelo Git; somente `.env.catalog-acceptance.example` é versionado.

Prepare o arquivo local com valores sintéticos fornecidos fora do repositório e confirme que as portas escolhidas não estão ocupadas:

```powershell
Copy-Item .env.catalog-acceptance.example .env.catalog-acceptance
# Edite .env.catalog-acceptance sem registrar os valores em histórico ou logs.
docker compose -p perfumestore-catalog-acceptance -f compose.catalog-acceptance.yaml --env-file .env.catalog-acceptance config --quiet
docker compose -p perfumestore-catalog-acceptance -f compose.catalog-acceptance.yaml --env-file .env.catalog-acceptance up -d postgres mailpit
```

O exemplo usa PostgreSQL em `127.0.0.1:56000`, SMTP em `127.0.0.1:11025`, Mailpit em `http://127.0.0.1:18025` e a aplicação em `127.0.0.1:18080`. Antes de iniciar a aplicação, carregue as variáveis do arquivo para a sessão do processo Java; o Spring Boot não carrega arquivos `.env` automaticamente. No provisionamento dedicado, defina `ADMIN_PROVISIONING_ENABLED=true`, forneça as demais variáveis `ADMIN_PROVISIONING_*` somente pela sessão e use `SERVER_PORT=0`:

```powershell
Get-Content .env.catalog-acceptance | Where-Object { $_ -and -not $_.StartsWith('#') } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    Set-Item -Path "Env:$name" -Value $value
}
$env:SERVER_PORT = '0'
Set-Location backend
.\mvnw.cmd --batch-mode spring-boot:run '-Dspring-boot.run.profiles=local,provision-admin'
```

O processo dedicado encerra depois de criar ou reconhecer o ADMIN sintético. Para iniciar a aplicação, desative `ADMIN_PROVISIONING_ENABLED`, use `SERVER_PORT=18080` e execute `spring-boot:run` com o perfil `local`. O smoke HTTP e a reinicialização usados na homologação estão registrados em `Docs/execucoes/FASE_04_1_*.md`; os logs longos locais ficam em `Docs/execucoes/logs/`.

Para parar somente os containers da homologação e preservar o volume, use `docker compose ... stop`. Para remover futuramente somente esse ambiente, depois de confirmar a decisão, use o mesmo projeto Compose com `down` sem afetar o `compose.yaml`, remova o volume nomeado do projeto e então o diretório `acceptance-data`; não use comandos globais de limpeza. O storage local foi validado, mas ainda não é um storage durável de produção.

### Estoque e disponibilidade — Fase 5

O estoque pertence à apresentação. `availableQuantity` é calculado pelo backend como `physicalQuantity - reservedQuantity`; uma apresentação sem saldo materializado começa em zero. O catálogo público expõe somente `availability.availableQuantity` e `availability.availableForPurchase`. Produto publicado sem estoque continua visível como indisponível.

Administração usa `GET /api/v1/admin/inventory/presentations/{presentationId}`, `POST /api/v1/admin/inventory/adjustments` e `GET /api/v1/admin/inventory/movements`. O POST exige sessão ADMIN, CSRF e header `Idempotency-Key`; `ENTRY` possui `quantity` positiva e `ADJUSTMENT` recebe delta inteiro não nulo, sem permitir que o físico fique abaixo do reservado. O histórico não é editável nem removível.

Reserva, confirmação e liberação são serviços internos de `InventoryService`, sem endpoints de teste. A Fase 5 não implementa expiração automática, pedido, checkout ou baixa por devolução. Os locks de saldo são PostgreSQL `FOR UPDATE`; o fluxo futuro de pedido deverá ordenar locks de múltiplas apresentações.

Para executar os testes com o JDK documentado:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Zulu\zulu-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Set-Location backend
.\mvnw.cmd --batch-mode clean verify
```

Os relatórios ficam em `backend/target/surefire-reports/`. A migration V4 é aplicada pelo Flyway; não use `clean`, `repair` ou `ddl-auto` contra banco de desenvolvimento. Os registros da execução estão em `Docs/execucoes/FASE_05_RELATORIO.md`, `FASE_05_COMANDOS.md` e `FASE_05_TESTES.md`.

### Homologação isolada do estoque — Fase 5

O smoke HTTP da Fase 5 usa um arquivo local `.env.phase5`, ignorado pelo Git, com PostgreSQL, volume, portas e credenciais sintéticas próprios. O ambiente usado nesta execução foi o projeto Compose `perfumestore-phase5`, com PostgreSQL em `127.0.0.1:56100`, Mailpit em `11125/18125` e aplicação em `18180`. Não reutilize o `.env` ou o volume do desenvolvimento.

Depois de preparar `.env.phase5` com valores locais sintéticos e confirmar as portas, valide e inicie apenas os serviços isolados:

```powershell
docker compose -p perfumestore-phase5 -f compose.catalog-acceptance.yaml --env-file .env.phase5 config --quiet
docker compose -p perfumestore-phase5 -f compose.catalog-acceptance.yaml --env-file .env.phase5 up -d postgres mailpit
```

O primeiro ADMIN deve ser criado pelo perfil dedicado já documentado na Fase 3, com `SERVER_PORT=0` e as variáveis `ADMIN_PROVISIONING_*` fornecidas somente pela sessão do processo. Para a aplicação normal, carregue as variáveis do arquivo, use `ADMIN_PROVISIONING_ENABLED=false` e `SERVER_PORT=18180`. O fluxo de reserva não é exposto por HTTP nesta fase; entradas e ajustes são exercitados por `POST /api/v1/admin/inventory/adjustments`, e a disponibilidade é conferida no catálogo público. Os testes automatizados cobrem reservas, confirmação, liberação e concorrência com PostgreSQL real.

Ao terminar, pare somente os serviços do projeto isolado com `docker compose -p perfumestore-phase5 -f compose.catalog-acceptance.yaml --env-file .env.phase5 stop postgres mailpit`; o volume próprio permanece para repetir a homologação. Não use `down -v`, `docker system prune` ou comandos globais de limpeza. Os logs locais e os dados sintéticos ficam fora do versionamento.

### Endereços e frete — Fase 6

A Fase 6 implementa endereços privados em GET/POST/PATCH/DELETE /api/v1/me/addresses e a consulta autenticada POST /api/v1/shipping/quotes. O CEP é validado e armazenado com oito dígitos, a UF é validada contra as UFs brasileiras e a remoção de endereço é lógica. O proprietário é sempre derivado da sessão; identificadores enviados pelo cliente não concedem acesso.

O endpoint de frete recebe o addressId do próprio cliente e calcula a cotação no backend. A regra inicial usa faixas de CEP configuráveis, valor decimal em BRL e prazo estimado em dias úteis. A cotação é persistida com validade e versão da regra, mas não cria pedido, reserva ou pagamento. A consulta exige sessão e CSRF porque usa um endereço privado e é uma operação POST.

Em desenvolvimento e testes, a configuração local pode usar valores sintéticos. O perfil prod permanece desabilitado por padrão e não oferece fallback de tarifa ou área. Antes de operar comercialmente, o proprietário deve definir e validar a área efetiva, tarifa, prazo e modalidade, fornecendo SHIPPING_ENABLED, SHIPPING_METHOD, SHIPPING_RULE_VERSION, SHIPPING_AMOUNT, SHIPPING_MIN_DAYS, SHIPPING_MAX_DAYS e SHIPPING_SERVED_RANGES por ambiente seguro. Configuração ausente ou inválida não oferece frete.

Para reproduzir a homologação HTTP isolada da Fase 6, prepare somente valores sintéticos no arquivo local ignorado:

    Copy-Item phase6-acceptance.env.example .env.phase6
    docker compose -p perfumestore-phase6 -f compose.phase6-acceptance.yaml --env-file .env.phase6 config --quiet
    docker compose -p perfumestore-phase6 -f compose.phase6-acceptance.yaml --env-file .env.phase6 up -d postgres mailpit

O ambiente usa PostgreSQL em 127.0.0.1:56200, SMTP em 127.0.0.1:11225, Mailpit em http://127.0.0.1:18225 e a aplicação em 127.0.0.1:18280. Carregue as variáveis no processo Java; o Spring Boot não lê .env automaticamente. O procedimento completo, incluindo smoke, reinicialização e comandos sanitizados, está em Docs/execucoes/FASE_06_*.md. Para preservar o volume, pare somente os serviços com docker compose ... stop; não use down -v.

Os testes da fase podem ser executados com backend\mvnw.cmd --batch-mode -Dtest=AddressShippingIntegrationTest test e a regressão completa com backend\mvnw.cmd --batch-mode clean verify. A Fase 6 não implementa carrinho, checkout, pedidos, pagamento ou integração externa de CEP/transportadora.

### Carrinho persistente - Fase 7

A Fase 7 implementa carrinhos persistentes para visitantes e clientes. O visitante recebe um token opaco `CART_TOKEN` somente na primeira mutacao; o token tem 32 bytes aleatorios, fica em cookie HttpOnly com Path `/api/v1` e somente seu hash SHA-256 e armazenado no banco. Em producao, o cookie deve usar HTTPS/Secure. Uma consulta anonima sem cookie retorna um carrinho virtual vazio e nao cria registro.

Endpoints implementados:

- `GET /api/v1/cart`: consulta o carrinho e recalcula preco, subtotal e disponibilidade.
- `POST /api/v1/cart/items`: adiciona quantidade a uma apresentacao elegivel; aceita `Idempotency-Key` opcional.
- `PATCH /api/v1/cart/items/{presentationId}`: define a quantidade absoluta e exige a versao atual do item.
- `DELETE /api/v1/cart/items/{presentationId}`: remove a linha; repeticao e segura.
- `POST /api/v1/cart/claim`: associa explicitamente o carrinho visitante a conta autenticada; o login tambem faz essa associacao.

O carrinho nunca reserva estoque. A disponibilidade e apenas informativa e vem do servico de estoque. O preco sempre vem do catalogo no servidor; o campo `priceReference` e somente uma referencia para informar `PRICE_CHANGED`, nunca uma autoridade comercial. O subtotal inclui apenas linhas publicadas e ainda nao inclui frete.

A associacao no login combina linhas iguais por soma, preserva linhas diferentes e e transacional. O token transferido e invalidado e o carrinho da conta permanece apos logout. As operacoes usam bloqueio de linha e unicidade no PostgreSQL para evitar linhas duplicadas e atualizacoes perdidas; conflitos de versao retornam `409`.

Limites tecnicos padrao: 50 linhas, 20 unidades por linha, TTL anonimo de 30 dias, TTL autenticado de 90 dias, retencao de limpeza de 7 dias e processamento de 100 carrinhos por lote. Sao limites de protecao do MVP, nao regras comerciais de quantidade. A limpeza e agendada e pode ser desabilitada por `CART_CLEANUP_ENABLED`; o ambiente local isolado da Fase 7 a deixou desabilitada durante o smoke.

Configuracao local isolada reproduzivel:

```powershell
Copy-Item phase7-acceptance.env.example .env.phase7-acceptance
docker compose -p perfumestore-phase7 -f compose.phase7-acceptance.yaml --env-file .env.phase7-acceptance config --quiet
docker compose -p perfumestore-phase7 -f compose.phase7-acceptance.yaml --env-file .env.phase7-acceptance up -d postgres mailpit
```

O exemplo usa PostgreSQL em `127.0.0.1:56300`, Mailpit em `127.0.0.1:18325/18326` e aplicacao em `127.0.0.1:18380`, com volume e credenciais sinteticamente exclusivos. O Spring Boot nao carrega `.env` sozinho; carregue as variaveis na sessao do processo Java. Pare somente os servicos deste projeto com `docker compose ... stop` para preservar o volume. Nao use `down -v`.

Para executar a regressao use o JDK 17 documentado:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Zulu\zulu-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Set-Location backend
.\mvnw.cmd --batch-mode clean verify
```

Os registros completos estao em `Docs/execucoes/FASE_07_RELATORIO.md`, `FASE_07_COMANDOS.md` e `FASE_07_TESTES.md`. A Fase 7 nao implementa pedidos, checkout, frete adicional, pagamento ou reserva de estoque.

### Fechamento técnico da Fase 8

Pedidos não pagos expiram conforme `ORDER_PAYMENT_TTL`/`app.order.payment-ttl`; o scheduler pode ser ajustado localmente por `ORDER_EXPIRATION_INTERVAL_MS` (milissegundos) e o lote por `ORDER_EXPIRATION_BATCH_SIZE`. A operação comercial real permanece bloqueada até provedor, webhooks, reconciliação e regras comerciais de frete serem definidos.

O smoke da Fase 8 usou PostgreSQL 18.6 e Mailpit isolados, com aplicação em `127.0.0.1:18088`, banco em `127.0.0.1:55432` e SMTP em `127.0.0.1:51025`. O volume `perfumestore-phase8-data-v2` é separado do desenvolvimento e foi preservado após parar os containers. O indicador de saúde do Mailpit foi desabilitado somente nessa execução local; liveness/readiness, banco, Flyway e health agregado foram verificados.
## Checkout e pedidos — Fase 8

O checkout autenticado está disponível em `POST /api/v1/checkout/orders` e exige `addressId`, `quoteId`, CSRF e o header `Idempotency-Key`. O backend recalcula valores, valida a cotação vigente, cria snapshots e reserva o estoque; adicionar ao carrinho continua sem reservar unidades.

Consultas usam `GET /api/v1/me/orders` e `GET /api/v1/me/orders/{publicNumber}`. O pagamento simulado existe apenas em `local` e `test`, em `POST /api/v1/dev/payment/orders/{publicNumber}`, com `APPROVED`, `REJECTED` ou `FAILED`; ele não representa cobrança real e não está disponível em produção.

Pedidos não pagos expiram conforme `ORDER_PAYMENT_TTL`/`app.order.payment-ttl`; o scheduler usa `ORDER_EXPIRATION_INTERVAL`/`app.order.expiration-interval`. A operação comercial real permanece bloqueada até provedor, webhooks e regras comerciais de frete serem definidos.

### Infraestrutura de pagamentos simulada - Fase 9

A Fase 9 adiciona uma fronteira interna `PaymentProvider`, persistência de tentativas, sessões de checkout, eventos, histórico e reconciliações. O adaptador `MOCK` está disponível somente nos perfis `local` e `test`; ele não representa uma cobrança real e não é habilitado em produção.

Endpoints implementados nos perfis aplicáveis:

- `POST /api/v1/me/orders/{publicNumber}/payment-sessions`: cria ou recupera uma sessão simulada, exigindo CUSTOMER, CSRF e `Idempotency-Key`.
- `GET /api/v1/me/orders/{publicNumber}/payment`: consulta a tentativa mais recente do próprio pedido.
- `POST /api/v1/webhooks/payments/mock`: entrada local/test de evento assinado com `X-Mock-Timestamp` e `X-Mock-Signature`.
- `POST /api/v1/dev/payment/attempts/{paymentId}/reconcile`: reconciliação local/test restrita a ADMIN.
- `POST /api/v1/dev/payment/orders/{publicNumber}`: fachada de compatibilidade do simulador da Fase 8, usando o mesmo núcleo de processamento.

O webhook MOCK assina os bytes `timestamp + "." + corpo bruto` com HMAC-SHA256 e segredo fornecido por `PAYMENT_MOCK_WEBHOOK_SECRET`. O evento é validado antes da persistência, identificado por `(provider, provider_event_id)` e processado sob lock persistido. Valor e moeda são comparados com o pedido; divergências entram em `REVIEW` sem confirmar estoque. Aprovação válida confirma as reservas pelo `InventoryService`, sem baixa duplicada. Aprovação após expiração mantém o pagamento aprovado, coloca o pedido em `PAYMENT_REVIEW` e não recria reserva.

Configuração local/teste usa `PAYMENT_ENABLED=true`, `PAYMENT_PROVIDER=MOCK`, TTL de checkout e segredo sintético. O perfil `prod` tem `PAYMENT_ENABLED=false`, não registra controllers MOCK por profile e não possui fallback: sem provedor real configurado, a operação falha de forma controlada e não confirma pedido nem estoque. A integração com provedor oficial, credenciais sandbox, autenticação real de webhooks e homologação financeira permanecem pendentes.

Para verificar a fase com JDK 17:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Zulu\zulu-17'
Set-Location backend
.\mvnw.cmd --batch-mode clean verify
```

Os registros completos da execução estão em `Docs/execucoes/FASE_09_RELATORIO.md`, `FASE_09_COMANDOS.md` e `FASE_09_TESTES.md`. O guia para substituição futura do adaptador está em `Docs/backend/05-integracao-provedor-pagamento.md`.
### Fase 10 — operação administrativa e pós-venda MOCK

## Fase 12 — homologacao integrada e preparacao operacional

A Fase 12 adiciona a imagem Docker multi-stage `Dockerfile`, executada como usuario nao-root, e o ambiente isolado `compose.phase12-acceptance.yaml` com PostgreSQL, Mailpit e Caddy para HTTPS local. O ambiente usa somente dados sinteticos, pagamento MOCK e frete sintetico; nao e producao.

Os endpoints `GET /actuator/health/liveness` e `GET /actuator/health/readiness` distinguem processo vivo de banco disponivel. O readiness inclui PostgreSQL; SMTP permanece desacoplado para que falhas de e-mail nao desfaçam operacoes comerciais.

O procedimento de backup/restore esta em `Docs/backend/08-operacao-deploy-backup-recuperacao.md` e nos scripts `scripts/phase12-backup.ps1` e `scripts/phase12-restore.ps1`. O backup PostgreSQL deve ser acompanhado do storage de imagens. A matriz de jornadas esta em `Docs/backend/07-homologacao-integrada.md`.

O campo `orderItemId` foi incluido nos itens de pedido para permitir que a administracao de devolucoes opere exclusivamente pela API, sem consulta SQL direta.

### Fase 11 - notificacoes transacionais

A aplicacao grava notificacoes de pedido recebido, pagamento aprovado e pedido enviado em uma outbox PostgreSQL (V10). O dispatcher envia depois do commit por SMTP, com `FOR UPDATE SKIP LOCKED`, lease, backoff, limite de tentativas e deduplicacao persistente. A consulta operacional ADMIN esta em `GET /api/v1/admin/notifications`.

No desenvolvimento, use Mailpit: SMTP `localhost:1025` e interface `http://localhost:8025`. Configure `NOTIFICATION_ENABLED=true`, `NOTIFICATION_DISPATCH_ENABLED=true`, `MAIL_HOST` e `MAIL_PORT`. Em `test`, o capturador em memoria e usado e o dispatcher agendado fica desligado. Em `prod`, notificacoes e dispatcher ficam desabilitados por padrao; habilite-os somente com SMTP transacional e variaveis externas configuradas. Mailpit nao e fallback produtivo.

Variaveis principais: `NOTIFICATION_ENABLED`, `NOTIFICATION_DISPATCH_ENABLED`, `NOTIFICATION_BATCH_SIZE`, `NOTIFICATION_MAX_ATTEMPTS`, `NOTIFICATION_INITIAL_BACKOFF`, `NOTIFICATION_MAX_BACKOFF`, `NOTIFICATION_LEASE_DURATION`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` e `MAIL_FROM`. Falhas de SMTP nao desfazem pedidos, pagamentos, reservas ou envios; ficam pendentes ou `FAILED` para diagnostico.

O guia operacional esta em `Docs/backend/06-notificacoes-operacao.md`. A recuperacao de senha mantem seu token apenas como hash e usa transporte especializado; nenhum token aparece na outbox, logs ou respostas.

Fechamento tecnico: a Fase 11 foi validada com 81 testes, 0 falhas, 0 erros e 0 ignorados. A Fase 12 reexecutou a suite com 82 testes aprovados e validou Docker, HTTPS local, smoke integrado, backup/restore e persistencia. O SMTP real e a operacao comercial de producao permanecem pendentes; a Fase 13 nao foi iniciada.
A administração de pedidos está disponível somente para `ADMIN` em `/api/v1/admin/orders`: listagem com filtros e ordenação permitida, detalhe, separação, envio, entrega e cancelamento. As transições preservam os estados separados de pedido, pagamento e entrega, exigem CSRF nas mutações e registram ator, motivo e correlação em histórico append-only.
O pós-venda diferencia cancelamento, reembolso e devolução física. Reembolsos são solicitados em `/api/v1/admin/orders/{publicNumber}/refunds` e somente podem ser confirmados pelo resultado do provedor MOCK em perfis `local`/`test`, por `/api/v1/dev/payment/refunds/{refundId}/simulate` ou reconciliação. O MOCK não existe no perfil `prod`, não há fallback financeiro e reembolso não repõe estoque.
Devoluções são administradas por `/api/v1/admin/orders/{publicNumber}/returns` e pelas operações `/receive`, `/inspect` e `/restock`. A reposição usa `InventoryService` com movimentação `RESTOCK`, somente após inspeção, com chave idempotente e sem duplicar unidades. A migration V9 cria os registros de logística, auditoria, reembolso e devolução sem alterar V1–V8.
Para homologação isolada, valide o Compose antes de iniciar e preserve o volume próprio:
```powershell
docker compose -p perfumestore-phase10-acceptance -f compose.phase10-acceptance.yaml --env-file phase10-acceptance.env.example config --quiet
docker compose -p perfumestore-phase10-acceptance -f compose.phase10-acceptance.yaml --env-file phase10-acceptance.env.example up -d postgres
```
O ambiente usa PostgreSQL em `127.0.0.1:56400` e o volume `perfumestore-phase10-post-sale-postgres-data`. Para parar sem remover dados, use `docker compose ... stop`; não use `down -v`. O procedimento e as evidências da execução estão em `Docs/execucoes/FASE_10_*.md`. A operação comercial real e os reembolsos financeiros reais continuam bloqueados.
