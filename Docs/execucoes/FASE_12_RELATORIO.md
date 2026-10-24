# FASE 12 - Relatorio de homologacao integrada

## Estado inicial

- Diretorio: `C:\Users\User\Desktop\Ecommerce PerfumeStore`.
- Branch: `main`.
- HEAD inicial: `45cf8966e70913701003fc5afee4ce4e5cd9c3cf`.
- Arvore inicial: limpa, sem arquivos nao rastreados e sem `origin`.
- Java direto: 8.0.491; Maven Wrapper: Maven 3.9.16 usando Java 17.0.16.
- PostgreSQL de desenvolvimento: container `perfumestore-postgres-1`, porta loopback `54329`, volume preservado e nao utilizado na homologacao.
- Docker: Engine 29.5.3; Compose 5.1.4.
- Migrations V1-V10 presentes e inalteradas.

## Objetivo e plano

Homologar o backend como conjunto, corrigir lacunas de integracao/operacao, preparar Docker/HTTPS local, comprovar backup/restore e registrar limites de producao. A homologacao utilizou projeto Compose isolado, PostgreSQL 18.6, Mailpit, Caddy com certificado interno e dados sinteticos.

## Decisoes adotadas

- Nao foi criada migration nova: V1-V10 ja cobrem o modelo e foram preservadas.
- `orderItemId` foi incluido no item do pedido administrativo para que devolucoes sejam operaveis exclusivamente pela API.
- Liveness nao depende do banco; readiness inclui o banco. SMTP permanece desacoplado da consistencia comercial.
- A imagem usa build multi-stage Java 17 e runtime nao-root.
- HTTPS foi terminado pelo Caddy apenas no ambiente isolado; o certificado interno nao e certificado publico.
- Backup logico PostgreSQL e backup do volume de imagens sao artefatos separados e devem ser coordenados pela mesma janela de consistencia.
- O perfil `prod` nao registra controllers/adaptador MOCK e o pagamento falha de forma controlada sem provedor real.

## Implementacoes e correcoes

- Adicionados `Dockerfile`, `.dockerignore`, Compose isolado, Caddyfile e exemplo de configuracao sintetica.
- Adicionados scripts PowerShell para `pg_dump`/`pg_restore` em banco novo.
- Adicionados documentos de homologacao e operacao.
- Adicionados grupos Actuator de liveness/readiness e `forward-headers-strategy`.
- Adicionado teste de contexto do perfil `prod` que verifica ausencia de `MockPaymentProvider`, controllers MOCK e uso de `UnavailablePaymentProvider`.
- Corrigido o contrato administrativo de itens de pedido com `orderItemId`.
- Corrigida a chave fallback de idempotencia de reembolso, que excedia `varchar(100)` em um cenario real de smoke.

## Evidencias executadas

- Baseline: 81 testes aprovados antes das alteracoes.
- Teste focado de isolamento produtivo: 1 teste aprovado.
- Suite final: 82 testes, 82 aprovados, 0 falhas, 0 erros, 0 ignorados; `clean verify` e JAR aprovados.
- Dockerfile construido e imagem executada como usuario `app` nao-root.
- Compose isolado subiu PostgreSQL, app, Mailpit e Caddy; os servicos principais ficaram saudaveis.
- HTTPS local respondeu health, liveness, readiness, CSRF e OpenAPI.
- Jornada HTTP/HTTPS comprovou cadastro, login, catalogo, imagem, estoque, carrinho anonimo, merge no login, endereco, frete, checkout, reserva/confirmacao, notificacoes no Mailpit, separacao, envio, entrega, cancelamento, reembolso MOCK, devolucao, inspecao e reposicao idempotente.
- Seguranca HTTP comprovou ADMIN autorizado, CUSTOMER proibido, anonimo nao autenticado e mutacao sem CSRF rejeitada.
- Perfil `prod` iniciado em container isolado: rota MOCK respondeu 404 mesmo com sessao ADMIN/CSRF; pagamento sem provedor respondeu 503 `PAYMENT_PROVIDER_UNAVAILABLE`; OpenAPI ficou desabilitada.
- Backup PostgreSQL gerado e restaurado em banco novo; a aplicacao iniciou sobre o banco restaurado com Flyway v10 e dados de pedidos, estoque, usuarios e notificacoes utilizaveis.
- Volume de imagens exportado para tar e validado.
- Reinicializacao de PostgreSQL/app preservou pedidos, estados e historico.

## Problemas encontrados e correcoes

1. A primeira execucao do teste de perfil produtivo falhou porque os IDs de exposicao Actuator estavam incorretos (`health/liveness`). A configuracao foi ajustada para expor somente `health`, com grupos `liveness` e `readiness`; o teste foi reexecutado com sucesso.
2. O teste de contexto sem servidor web nao materializava o `HttpSecurity` esperado. O teste foi alterado para `RANDOM_PORT` e passou.
3. O primeiro build Docker tentou executar `./mvnw` com incompatibilidade de CRLF/shebang no Linux. O Dockerfile passou a executar Maven instalado na imagem builder; o build passou.
4. O primeiro Caddyfile com site wildcard produziu alerta TLS interno. O site foi definido explicitamente para `localhost`; HTTPS passou no host e em container.
5. Os scripts PowerShell usaram `-AsByteStream`, indisponivel no Windows PowerShell 5. Foram corrigidos para redirecionamento binario via `cmd.exe`; backup e restore passaram.
6. O primeiro pedido de reembolso no smoke excedeu o limite de `varchar(100)` da chave fallback. A chave passou a usar digest estavel; o teste de pos-venda e o smoke foram reexecutados com sucesso.
7. Houve uma tentativa de cadastro com JSON inline mal escapado no PowerShell e uma tentativa de entrega com URL contendo espaco. Ambos foram repetidos com payload/URL corretos; os fluxos passaram.
8. Uma tentativa inicial de provisionamento usou placeholder literal de senha curto; foi repetida com dado sintetico valido. Nenhuma credencial real foi utilizada.

## Limitacoes

- Provedor financeiro real, SMTP real, frete comercial definitivo, storage produtivo e hospedagem publica nao foram integrados.
- Pagamento MOCK, Mailpit, frete sintetico e certificado interno sao exclusivos da homologacao.
- O backup de imagens e o dump PostgreSQL sao artefatos separados; nao existe snapshot atomico de storage e banco.
- O SMTP convencional nao oferece garantia universal de entrega exactly-once.
- O frontend ainda nao foi integrado; os contratos foram revisados para permitir a integracao futura.

## Estado final

- Backend tecnicamente homologado em ambiente isolado.
- Dados e volumes de desenvolvimento preservados.
- Servicos Compose da Fase 12 mantidos ativos para inspeccao; containers auxiliares de producao/restauracao foram removidos.
- Artefatos de backup permanecem fora do Git e ignorados.
- Nenhuma alteracao ocorreu nas migrations V1-V10.

## Commit

- Mensagem: `chore: complete integrated backend acceptance and operations readiness`.
- Commit local unico; o hash final foi confirmado por `git rev-parse HEAD` no encerramento.
- Arvore final limpa, sem push, deploy, tag de producao ou inicio da Fase 13.
