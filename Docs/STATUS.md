# PerfumeStore — status do projeto

## Fase atual

Fase 4 — catálogo, categorias, apresentações e imagens.

## Entregas produzidas

- Ajustes pontuais na especificação da Fase 0 para separar solicitação de reembolso, evidência/reconciliação e confirmação efetiva pelo provedor, além de explicitar a reposição física condicionada à situação da mercadoria.
- Backend Spring Boot em `backend/`, com Maven Wrapper, perfis de configuração, packages de domínio preparados e nenhuma funcionalidade comercial simulada.
- PostgreSQL local em `compose.yaml`, com imagem fixada, volume persistente, healthcheck e porta publicada somente no loopback.
- `.env.example`, `.gitignore` e `.gitattributes` para separar configuração local, credenciais e artefatos.
- Flyway com a migration `V1__initialize_application_schema.sql`, que cria somente o schema lógico `perfumestore`.
- Actuator expondo apenas `/actuator/health`, sem detalhes de componentes ou credenciais.
- Teste de integração com PostgreSQL real via Testcontainers, verificando migration, histórico e saúde HTTP.
- Workflow `.github/workflows/backend-ci.yml` para push e pull request, ainda não executado no GitHub.
- `README.md` com pré-requisitos, comandos PowerShell/Linux, operação do Compose, testes, build, health check e solução de problemas.
- Contrato compartilhado de erros em `application/problem+json`, validação de corpos e parâmetros, correlação de requisições, paginação, ordenação por whitelist e serialização de dinheiro/datas.
- Spring Security restritivo, sem login comercial, sem usuário fixo e sem senha gerada; rotas protegidas por padrão, handlers JSON para `401`/`403` e CSRF por cookie preparado para a Fase 3.
- CORS com origens explícitas por ambiente, endpoint técnico de CSRF e OpenAPI em local/test; documentação desabilitada no perfil `prod`.
- Fixtures de teste somente em `src/test/java` e 12 testes de contrato HTTP com PostgreSQL real via Testcontainers.

## Versões utilizadas

- Java 17.0.16 (Zulu).
- Spring Boot 4.1.1.
- Maven Wrapper 3.3.4 com Maven 3.9.16.
- PostgreSQL 18.6 para Compose e Testcontainers.
- Testcontainers for Java 2.0.5.
- Flyway gerenciado pelo Spring Boot 4.1.1, usando `spring-boot-starter-flyway` e o suporte PostgreSQL correspondente.
- springdoc OpenAPI Starter WebMVC 3.1.1.

## Ajustes documentais da Fase 0

- Reembolso solicitado permanece distinto de reembolso confirmado; a reconciliação só confirma com evidência compatível e verificável.
- Cancelamento libera uma reserva ativa no máximo uma vez.
- Reembolso financeiro não repõe estoque automaticamente.
- Mercadoria enviada só pode voltar ao estoque vendável após recebimento e inspeção aplicáveis.
- Reprocessamentos de cancelamento, reembolso, devolução e estoque devem ser idempotentes.

## Comandos executados e resultados

- `git init -b main`: repositório local inicializado na raiz correta.
- `backend\mvnw.cmd --version`: Maven Wrapper funcionando com Maven 3.9.16 e Java 17.0.16.
- `docker compose config --quiet`: configuração do Compose válida.
- `docker compose up -d postgres`: PostgreSQL 18.6 iniciado e reportado como `healthy` em `127.0.0.1:54329`.
- `backend\mvnw.cmd --batch-mode --no-transfer-progress clean verify`: **BUILD SUCCESS**; 2 testes, 0 falhas, 0 erros, JAR executável gerado.
- `backend\mvnw.cmd -Dtest=ApiContractIntegrationTest test`: **BUILD SUCCESS**; 12 testes de contrato, 0 falhas, 0 erros, com PostgreSQL 18.6 real via Testcontainers.
- A suíte de contrato confirmou erros JSON, validação, paginação/ordenação, serialização, correlação, `401`/`403`, CORS, CSRF, OpenAPI e health. O contexto aplicou a migration V1 no banco isolado.
- Smoke test do JAR com perfil `local`: aplicação iniciou em 8080, aplicou V1 e `/actuator/health` respondeu HTTP 200 com status `UP`.
- Consulta no banco do Compose: histórico do Flyway registrou V1 com `success = true` e o schema `perfumestore` existe.
- Segunda inicialização contra o mesmo volume: Flyway validou as migrations e informou que o schema estava atualizado, sem reaplicar V1.

Os logs observados não exibiram senhas, tokens ou cookies. O workflow de CI foi revisado localmente quanto a caminhos e comandos, mas não foi executado pelo GitHub nesta etapa.

## Pendências e limitações

Catálogo, estoque comercial, carrinho, frete, pedidos, pagamentos e notificações comerciais ainda não foram implementados. Cadastro, login, autenticação persistida, perfil e recuperação pertencem à entrega atual. As pendências comerciais e de serviços externos continuam classificadas em `Docs/backend/03-decisoes-e-pendencias.md`.

O ambiente local possui Java 8 no `PATH` de `java`, embora o JDK 17 esteja disponível em `C:\Program Files\Zulu\zulu-17`; o README documenta a correção por sessão. A identidade efetiva usada no commit foi `Davi Esteves <davi00eteves@gmail.com>`; nenhuma identidade global foi alterada.

## Situação da Fase 1

Implementação e verificação local concluídas. A conclusão refere-se à fundação executável e à infraestrutura desta fase; não marca funcionalidades comerciais futuras como prontas.

## Situação da Fase 2

Implementação e verificação local concluídas. A conclusão refere-se aos padrões compartilhados de API, à proteção padrão das rotas, CORS, CSRF técnico, OpenAPI e seus testes; não marca autenticação comercial nem módulos de negócio como prontos.

## Próxima etapa

Iniciar a Fase 4 para implementar catálogo, categorias e apresentações, usando a autenticação e os contratos compartilhados como referência. A Fase 4 não foi iniciada automaticamente.

## Atualização — Fase 4

### Entregas

- Migration incremental V3 para categorias, produtos, apresentações e imagens, sem tabelas de estoque, carrinho ou pedidos.
- Administração protegida por `ADMIN`, com DTOs específicos, validação, unicidade e concorrência otimista por `version`.
- Catálogo público paginado com busca por nome, filtro por categoria, ordenação por whitelist e detalhes somente de produtos publicados.
- Publicação derivada de categoria ativa, produto ativo, apresentação ativa e imagem principal única.
- Upload local persistente de PNG/JPEG com limite de 5 MiB, 8 imagens ativas por produto, limite de 20 milhões de pixels, chaves geradas pelo servidor e proteção contra path traversal.
- Imagem principal, ordenação, remoção lógica e conteúdo público/admin com políticas de acesso distintas.
- `CatalogIntegrationTest` com PostgreSQL 18.6 real via Testcontainers.

### Evidências

- Compilação e `CatalogIntegrationTest`: 4 testes, 0 falhas, 0 erros, 0 ignorados.
- `clean verify` final: 31 testes, 0 falhas, 0 erros, 0 ignorados; JAR executável gerado e V3 aplicada nos bancos isolados.
- Logs brutos sanitizáveis permanecem em `Docs/execucoes/logs/`, ignorados pelo Git.

### Limitações e pendências

O storage local foi validado; storage durável/CDN, rotina de reconciliação de arquivos órfãos e política final de URLs/cache de produção permanecem pendentes. Estoque, disponibilidade, carrinho, checkout, pedidos e frontend não foram implementados.

### Situação

Fase 4 implementada, validada localmente e registrada no commit local da entrega. O smoke administrativo contra o banco persistente ficou parcial por ausência de credencial do ADMIN existente; os fluxos equivalentes passaram nos testes com PostgreSQL real. A conclusão não marca o sistema como pronto para produção.

### Próxima etapa

Fase 5 do roadmap, sem início automático nesta entrega.

## Evidências da revisão

- `Docs/ROADMAP_BACKEND.md` e `Docs/ROADMAP_FRONTEND.md` foram lidos e preservados.
- Os seis documentos da Fase 0 foram consultados antes da implementação.
- Não havia `AGENTS.md` nem implementação anterior; os documentos existentes foram mantidos.
- O diff da Fase 2 deve ficar restrito aos padrões compartilhados, configuração de segurança, testes de contrato e documentação correspondente; a V1 e os módulos comerciais permanecem inalterados.
- Commit local criado com a mensagem solicitada; o hash é informado no relatório da entrega.
## Atualização — Fase 3

### Entregas

- Migration V2 incremental com usuários, tokens de recuperação e tabelas oficiais do Spring Session JDBC.
- Cadastro CUSTOMER, login, logout, sessão persistida, rotação de identificador, CSRF renovado e consulta/atualização limitada do perfil.
- Recuperação de senha com token aleatório de uso único, hash persistido, expiração, consumo atômico, invalidação de sessões e e-mail local/teste.
- Provisionamento ADMIN dedicado, desabilitado por padrão, idempotente e sem endpoint público.
- Limitação configurável de tentativas com `429` e `Retry-After`.
- Mailpit no Compose local e perfil `provision-admin` sem servidor web.
- Testes PostgreSQL reais, testes de concorrência e smoke HTTP documentados nos três arquivos de `Docs/execucoes/`.

### Evidências

- `AuthenticationIntegrationTest`: 10 testes aprovados.
- `AdminProvisioningIntegrationTest`: 2 testes aprovados.
- `SessionPersistenceIntegrationTest`: 1 teste aprovado com HTTP real e sessão JDBC.
- Regressão da Fase 1/2 preservada; `clean verify` terminou com 27 testes, 0 falhas, 0 erros e JAR executável; smoke local registrou `200/201/200/200/200/204/401/202/204/401/200` na sequência documentada.
- Logs brutos sanitizáveis permanecem em `Docs/execucoes/logs/`, ignorados pelo Git.

### Limitações e pendências

Mailpit é exclusivo do desenvolvimento. Provedor de e-mail, topologia HTTPS/domínios, MFA, verificação de e-mail e rate limiting distribuído continuam pendentes. O limitador em memória não é suficiente para múltiplas instâncias. Catálogo e demais fases comerciais não foram iniciados.

### Situação

Fase 3 concluída quanto ao escopo local de autenticação, usuários, sessão e recuperação, condicionada às verificações registradas. A conclusão da especificação/implementação não marca o produto como pronto para produção.

### Próxima etapa

Fase 4 do roadmap, sem início automático nesta entrega.
