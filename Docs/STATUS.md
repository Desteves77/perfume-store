# PerfumeStore — status do projeto

## Fase atual

Fase 4.1 — homologação isolada do catálogo.

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

## Atualização — Fase 5: estoque e disponibilidade

### Entregas

- Migration incremental `V4__create_inventory_tables.sql` com saldo por apresentação, reservas e movimentações auditáveis.
- Entradas positivas e ajustes físicos por delta não nulo, com idempotência por `Idempotency-Key`, motivo obrigatório e responsável ADMIN; reduções abaixo do reservado são rejeitadas.
- Serviços internos transacionais para reservar, confirmar e liberar; não foram criados endpoints públicos para reservas.
- Concorrência protegida por bloqueios PostgreSQL `FOR UPDATE`, constraints e chaves únicas; a disponibilidade é `physical - reserved`.
- Catálogo público integrado com `availability.availableQuantity` e `availability.availableForPurchase`, sem exposição de físico, reservado ou dados administrativos.
- Documentação, testes focados, registros de execução e build atualizados.

### Evidências

- `InventoryIntegrationTest`: 8 testes aprovados, 0 falhas, 0 erros e 0 ignorados.
- `clean verify`: 41 testes aprovados, 0 falhas, 0 erros e 0 ignorados; JAR executável gerado.
- Smoke HTTP isolado: 16 cenários aprovados com ADMIN, CUSTOMER, anônimo, CSRF, idempotência, histórico, OpenAPI e catálogo público.
- Restart: health 200, saldo físico 5, reservado 0, disponível 5, disponibilidade pública 5 e uma movimentação preservados; Flyway validou V1–V4 sem reaplicação.
- Provisionamento ADMIN sintético executado duas vezes com código 0; a segunda execução foi idempotente. PostgreSQL e Mailpit dedicados foram parados com `stop`, sem remoção do volume.

### Limites e próxima etapa

Expiração automática, pedidos, checkout, carrinho, baixa por devolução física e reconciliação operacional permanecem fora da Fase 5 e serão tratados nas fases correspondentes. A Fase 5 está concluída e validada quanto ao escopo local documentado; a Fase 6 não foi iniciada.

Commit local previsto para esta entrega: `feat: implement inventory management and stock reservations`. Push e deploy não foram realizados.

## Atualização — Fase 5

### Estado da implementação

A implementação da Fase 5 foi criada e posteriormente validada. A migration V4, o domínio `inventory`, os endpoints administrativos de saldo/entrada/histórico e a integração de disponibilidade no catálogo foram criados. Reservas, confirmação e liberação são serviços internos; carrinho, pedidos, checkout e expiração automática não foram iniciados.

### Evidências parciais

- JDK 17.0.16 confirmado pelo Maven Wrapper.
- Docker Desktop foi iniciado sem alterar o Compose de desenvolvimento nem volumes existentes.
- `InventoryIntegrationTest`: 8 testes, 0 falhas, 0 erros e 0 ignorados, incluindo migration V4, HTTP/CSRF, disponibilidade pública, reserva concorrente da última unidade e transições terminais concorrentes.
- Compilação principal e de testes aprovadas; os resultados finais de `clean verify`, smoke HTTP e persistência após reinicialização estão registrados na atualização final desta fase.

### Próxima verificação

Executar a suíte completa, revisar OpenAPI e contratos, realizar smoke isolado quando possível, registrar resultados e criar o commit local da Fase 5. A Fase 6 não será iniciada automaticamente.

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

## Atualização — Fase 4.1

### Entregas e ambiente

- Homologação HTTP concluída em ambiente isolado criado pelo projeto Compose `perfumestore-catalog-acceptance`.
- PostgreSQL próprio em `127.0.0.1:56000`, volume próprio, Mailpit em `11025/18025`, aplicação em `18080` e imagens em `acceptance-data/catalog-images`.
- ADMIN sintético provisionado pelo mecanismo dedicado da aplicação; segunda execução retornou código `0` e manteve exatamente um ADMIN. O processo dedicado encerrou e não deixou a porta HTTP aberta.
- Smoke administrativo e público executado com sessão real, CSRF real e sessões HTTP separadas para ADMIN, CUSTOMER e anônimo.
- PostgreSQL e aplicação reiniciados sem remoção do volume; dados, estados, ordem, imagem principal e checksums foram preservados.
- Dois testes focados foram adicionados ao `CatalogIntegrationTest` para escala/concorrência otimista e desativação de categoria.

### Evidências

- Smoke HTTP: criação de categoria, produto, apresentações, duas imagens, publicação, busca, filtro, detalhe, preço, desativação de apresentação, ordem/imagem principal, permissões, CSRF e ocultação de produto inativo aprovados.
- Flyway no ambiente isolado: baseline, V1, V2 e V3 com `success=true`; reinicialização informou schema atualizado sem reaplicação.
- `CatalogIntegrationTest`: 6 testes, 0 falhas, 0 erros, 0 ignorados.
- `clean verify`: 33 testes, 0 falhas, 0 erros, 0 ignorados; JAR executável gerado.
- Registros detalhados: `Docs/execucoes/FASE_04_1_RELATORIO.md`, `FASE_04_1_COMANDOS.md` e `FASE_04_1_TESTES.md`.

### Limitações e situação

O storage local e a política atual de acesso foram homologados. Storage durável/CDN, limpeza operacional de arquivos órfãos e URLs finais de produção continuam pendentes. Estoque, disponibilidade, carrinho, checkout, pedidos e frontend permanecem fora desta fase.

A Fase 4 está fechada quanto ao escopo de catálogo local e sua homologação isolada. A conclusão não marca o sistema como pronto para produção e não inicia a Fase 5.

### Próxima etapa

Fase 5 do roadmap, sem início automático nesta entrega.

## Atualização — Fase 6

### Entregas

- Endereços persistidos por cliente na V5, com CEP normalizado, UF validada, paginação, atualização otimista e remoção lógica.
- Endpoints GET/POST/PATCH/DELETE /api/v1/me/addresses com isolamento por sessão e CSRF nas escritas.
- Serviço de frete configurável por faixas de CEP, persistência de cotação, BRL decimal, prazo estimado e versão da regra.
- Endpoint autenticado POST /api/v1/shipping/quotes, sem dependência de carrinho ou pedido.
- Configurações separadas para local, teste e produção; produção não oferece tarifa ou faixa de fallback.
- Ambiente Compose isolado, com PostgreSQL, Mailpit, volume e portas próprios, preservado parado para reprodução.

### Evidências

- AddressShippingIntegrationTest: 8 testes aprovados com PostgreSQL real, incluindo validação, IDOR, CSRF, cotação e configuração.
- clean verify: 49 testes aprovados, 0 falhas, 0 erros e 0 ignorados; JAR executável gerado.
- Smoke HTTP isolado: 19 cenários aprovados, incluindo dois clientes, endereço próprio, tentativa de acesso cruzado, CSRF, cotação atendida/não atendida, remoção lógica e persistência.
- Reinicialização do PostgreSQL e da aplicação preservou 6 endereços ativos, 8 cotações e Flyway na versão 5; V1–V4 permaneceram intactas.

### Limitações e pendências

A área de atendimento, tarifa, modalidade e prazo reais continuam pendentes de decisão comercial. A configuração sintética usada no perfil local/teste não é uma condição comercial homologada e não habilita o perfil de produção. Integração de CEP, transportadora, carrinho, checkout, pedido e pagamento permanecem nas fases posteriores.

### Situação

Implementação técnica da Fase 6 validada localmente. A configuração comercial real de entrega permanece pendente. A Fase 7 não foi iniciada.

## Atualizacao - Fase 7: carrinho persistente

- Carrinhos anonimos usam token opaco em cookie HttpOnly; apenas o hash SHA-256 e persistido em V6. Sem cookie, `GET /api/v1/cart` retorna vazio sem criar registro.
- Carrinhos autenticados sao identificados pela sessao e possuem um unico carrinho ativo por cliente. O logout invalida a sessao, limpa o cookie visitante e preserva o carrinho da conta.
- O login combina o carrinho visitante com o carrinho da conta em uma transacao. Linhas da mesma apresentacao somam quantidades; linhas diferentes sao preservadas; o token transferido nao pode ser reutilizado.
- Precos e disponibilidade sao reconsultados no servidor em cada operacao. O carrinho nao reserva estoque e nao altera fisico, reservado ou historico de movimentacoes.
- V6 cria `carts`, `cart_items` e `cart_mutation_keys`, com unicidade por proprietario ativo, token e apresentacao. O estado transferido e expirado e preservado para auditoria/limpeza segura.

### Evidencias da execucao

- `CartIntegrationTest`: 9 testes aprovados com PostgreSQL real, incluindo persistencia, idempotencia, merge, claim concorrente, CSRF, isolamento, concorrencia de inclusao e limpeza.
- `clean verify`: 58 testes aprovados, 0 falhas, 0 erros e 0 ignorados; JAR executavel gerado.
- Smoke HTTP isolado: 18 cenarios aprovados, incluindo carrinho anonimo, cookie, operacoes, merge no login, logout, isolamento, preco vigente, estoque indisponivel, token invalido e health.
- Reinicio da aplicacao e do PostgreSQL isolados preservou carrinhos, itens e V6; a consulta final confirmou `reserved_quantity=0` e nenhum movimento de estoque causado pelo carrinho.
- OpenAPI respondeu `200` e publicou os quatro caminhos reais de carrinho.

### Situacao

Fase 7 implementada e validada localmente. A Fase 8 continua responsavel por checkout, pedido, snapshot de preco/frete e reserva de estoque. O carrinho nao e garantia de compra.

## Atualizacao - Fase 8: checkout e pedidos

- Migration `V7__create_order_tables.sql` adiciona pedidos, itens, snapshots de endereço/frete, referências de reservas, histórico e idempotência por cliente. V1–V6 não foram alteradas.
- Checkout CUSTOMER em `POST /api/v1/checkout/orders`; consultas em `GET /api/v1/me/orders` e `GET /api/v1/me/orders/{publicNumber}`. Todos os valores são recalculados no backend.
- Reservas de múltiplas apresentações são realizadas atomicamente em ordem determinística de UUID. Falha em qualquer item desfaz pedido, snapshots, reservas e histórico.
- Pedido inicia como `PENDING_PAYMENT`, pagamento `PENDING` e entrega `NOT_STARTED`; expiração libera reservas de forma idempotente. O simulador HTTP existe somente em `local`/`test`.
- Regressão final: 64 testes, 0 falhas, 0 erros, 0 ignorados; `clean verify` aprovado e JAR executável gerado.
- Smoke HTTP isolado: health 200, login/claim de carrinho, endereço, cotação, pedido BRL 70,90, replay idempotente, aprovação simulada, 401 anônimo, 403 sem CSRF e persistência após reinicialização.
- A operação comercial real segue bloqueada por provedor de pagamento/webhooks/reconciliação e regras comerciais reais de frete.
## Fase 8 — Checkout e criação de pedidos

Implementação técnica em andamento nesta execução: migration V7, pedidos com snapshots, checkout autenticado, reserva atômica de múltiplas apresentações, idempotência em PostgreSQL, consulta de histórico, expiração e simulador restrito a `local`/`test` foram criados. Os testes focados de PostgreSQL real passaram, incluindo concorrência.

A Fase 8 só será marcada como concluída após `clean verify`, smoke HTTP contra o JAR final, validação de persistência/reinicialização, revisão do diff e commit local. A Fase 9 não foi iniciada.
## Fase 9 - infraestrutura de pagamentos MOCK

Estado: implementação técnica concluída em validação local; integração com provedor real pendente.

Entregas:

- Migration V8 para tentativas, histórico, eventos, reconciliações e estado persistido do MOCK.
- `PaymentProvider`, `MockPaymentProvider` e adaptador indisponível fora de `local`/`test`.
- Sessão de checkout, consulta de pagamento, webhook MOCK HMAC, reconciliação e fachada compatível da Fase 8.
- Idempotência por pedido/chave e por `(provider, provider_event_id)`, locks PostgreSQL e tratamento de eventos fora de ordem.
- Aprovação válida confirma reservas pelo serviço de pedidos/estoque; aprovação tardia usa `PAYMENT_REVIEW` sem recriar estoque.
- Documentação de contratos, decisões, validação e substituição futura do provedor.

Evidências já obtidas:

- `PaymentIntegrationTest`: 5 cenários em PostgreSQL real, após correções, sem falhas.
- `OrderIntegrationTest`: 6 cenários regressivos, sem falhas.
- V1-V8 aplicadas em bancos Testcontainers; V1-V7 permaneceram inalteradas.

Pendente antes do encerramento formal: executar `clean verify`, smoke HTTP isolado contra o JAR final e validar o perfil `prod` sem controllers/adaptador MOCK. O resultado final e o commit serão registrados nos documentos `Docs/execucoes/FASE_09_*.md`.

Não iniciar a Fase 10 nesta etapa.
### Atualização final — Fase 9

Estado: infraestrutura MOCK concluída e validada localmente; integração com provedor real pendente.

Evidências finais: `clean verify` após a correção de `application-prod.yml` passou com 69 testes, 0 falhas, 0 erros e 0 ignorados; smoke HTTP isolado confirmou health, checkout MOCK, webhook HMAC, idempotência, pedido confirmado e baixa única; perfil `prod` respondeu health 200, `/v3/api-docs` 404 e rota MOCK 404; V1–V8 foram aplicadas e o volume isolado foi preservado parado.

Foi corrigido um defeito de configuração no perfil `prod`: duas chaves YAML de topo `spring` impediam a inicialização. O bloco `spring.mail` foi incorporado ao bloco existente, sem habilitar pagamento ou MOCK em produção.

O commit local e o estado final serão registrados após a revisão de diff e segredos. A Fase 10 não foi iniciada.
Commit local da Fase 9 criado com a mensagem `feat: implement mock payment provider infrastructure`; o hash final foi verificado por `git rev-parse HEAD` e é informado no handoff. A árvore ficou limpa após o commit; não houve push, deploy ou início da Fase 10.
## Fase 10 — operação administrativa e pós-venda MOCK

A implementação técnica inclui administração de pedidos, separação, envio, entrega, cancelamento, reembolsos e devoluções com inspeção e reposição condicionada. A migration V9 foi criada sem alterar V1–V8. Os testes focados da fase passaram (6/6) e a suíte completa passou (75/75); a homologação HTTP isolada e a revisão final serão registradas neste documento antes do commit.

### Atualização final — Fase 10

## Fase 12 — homologacao integrada e preparacao operacional

Concluida tecnicamente em ambiente isolado: suite final com 82 testes aprovados, Docker multi-stage nao-root, Compose com PostgreSQL/Mailpit/Caddy HTTPS, health liveness/readiness, smoke integrado, backup/restore comprovado e documentacao operacional. As evidencias estao em `Docs/execucoes/FASE_12_*.md`.

Esta fase nao habilita producao: pagamento MOCK, frete sintetico, Mailpit e certificado local sao exclusivos de homologacao. Provedor financeiro real, SMTP produtivo, storage de imagens e regras comerciais definitivas permanecem pendentes.

## Fase 11 - notificacoes transacionais

A Fase 11 adiciona uma outbox persistente em V10 para `ORDER_RECEIVED`, `PAYMENT_APPROVED` e `ORDER_SHIPPED`. Os eventos sao deduplicados por chave logica dentro da transacao comercial; o envio SMTP ocorre depois, por processador com lease PostgreSQL, `SKIP LOCKED`, tentativas limitadas, backoff e estado terminal.

O transporte local usa Mailpit e o perfil `test` usa capturador em memoria. O perfil `prod` mantem notificacoes desabilitadas por padrao e nao usa Mailpit como fallback. Recuperacao de senha continua especializada para manter o token somente como hash; a mensagem foi revisada sem colocar o token recuperavel na outbox.

A implementacao e a validacao da fase estao registradas em `Docs/execucoes/FASE_11_*.md`. A Fase 12 foi concluida tecnicamente sem liberar producao.

### Fechamento tecnico da Fase 11

V10 e a outbox transacional foram validadas com 81 testes, 0 falhas, 0 erros e 0 ignorados. O smoke contra o JAR final capturou no Mailpit pedido recebido, pagamento aprovado e pedido enviado; retry, deduplicacao, rollback, claim concorrente e escape HTML foram cobertos. O PostgreSQL isolado foi reiniciado sem perda, Flyway permaneceu na V10 e os containers foram preservados para reproducao.

O SMTP real, a integracao financeira real e a operacao comercial de producao continuam pendentes. A Fase 12 validou apenas a preparacao tecnica isolada.

Smoke HTTP isolado contra o JAR final aprovado no ambiente Compose `perfumestore-phase10-smoke11`: pedido aprovado, separação, envio, entrega, devolução com inspeção/reposição idempotente, reembolso MOCK sem reposição automática, cancelamento com liberação de reserva, autorização ADMIN/CUSTOMER/anônimo e CSRF. A aplicação e o PostgreSQL foram reiniciados sem perda; Flyway validou V1–V9. O perfil `prod` iniciou com pagamentos desabilitados, sem controllers MOCK e sem OpenAPI público.

Resultado: implementação técnica da Fase 10 concluída e validada localmente. Reembolsos financeiros reais, políticas comerciais definitivas e operação de produção permanecem pendentes. A Fase 11 não foi iniciada.
