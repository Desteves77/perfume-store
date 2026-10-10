# PerfumeStore — status do projeto

## Fase atual

Fase 1 — inicialização do backend e infraestrutura local.

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

## Versões utilizadas

- Java 17.0.16 (Zulu).
- Spring Boot 4.1.1.
- Maven Wrapper 3.3.4 com Maven 3.9.16.
- PostgreSQL 18.6 para Compose e Testcontainers.
- Testcontainers for Java 2.0.5.
- Flyway gerenciado pelo Spring Boot 4.1.1, usando `spring-boot-starter-flyway` e o suporte PostgreSQL correspondente.

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
- Smoke test do JAR com perfil `local`: aplicação iniciou em 8080, aplicou V1 e `/actuator/health` respondeu HTTP 200 com status `UP`.
- Consulta no banco do Compose: histórico do Flyway registrou V1 com `success = true` e o schema `perfumestore` existe.
- Segunda inicialização contra o mesmo volume: Flyway validou as migrations e informou que o schema estava atualizado, sem reaplicar V1.

Os logs observados não exibiram senhas, tokens ou cookies. O workflow de CI foi revisado localmente quanto a caminhos e comandos, mas não foi executado pelo GitHub nesta etapa.

## Pendências e limitações

Não foram implementados autenticação, Spring Security, usuários, catálogo, estoque comercial, carrinho, frete, pedidos, pagamentos, notificações, OpenAPI ou endpoints de negócio. As pendências comerciais e de serviços externos continuam classificadas em `backend/03-decisoes-e-pendencias.md` e não bloqueiam a base técnica da Fase 1.

O ambiente local possui Java 8 no `PATH` de `java`, embora o JDK 17 esteja disponível em `C:\Program Files\Zulu\zulu-17`; o README documenta a correção por sessão. A identidade efetiva usada no commit foi `Davi Esteves <davi00eteves@gmail.com>`; nenhuma identidade global foi alterada.

## Situação da Fase 1

Implementação e verificação local concluídas. A conclusão refere-se à fundação executável e à infraestrutura desta fase; não marca funcionalidades comerciais futuras como prontas.

## Próxima etapa

Revisar e, se necessário, confirmar pendências de serviços externos e iniciar a Fase 2 prevista no roadmap, mantendo a especificação e esta fundação como referência. A Fase 2 não foi iniciada automaticamente.

## Evidências da revisão

- `Docs/ROADMAP_BACKEND.md` e `Docs/ROADMAP_FRONTEND.md` foram lidos e preservados.
- Os seis documentos da Fase 0 foram consultados antes da implementação.
- Não havia `AGENTS.md` nem implementação anterior; os documentos existentes foram mantidos.
- O diff final deve ficar restrito à documentação ajustada e aos arquivos da fundação backend desta fase.
- Commit local criado com a mensagem solicitada; o hash é informado no relatório da entrega.
