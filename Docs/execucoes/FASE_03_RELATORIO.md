# Fase 3 — relatório técnico de execução

## Estado inicial

- Diretório: `C:\Users\User\Desktop\Ecommerce PerfumeStore`.
- HEAD: `b9a0a0f90d5b9d288e291586972393863fc0f701` (`feat: establish API standards and security foundation`).
- Working tree inicial limpo; não havia `AGENTS.md`.
- Fase 2 presente com Java 17, Spring Boot 4.1.1, PostgreSQL 18.6, Flyway V1, Spring Security, CSRF/CORS, OpenAPI e 14 testes anteriores.
- Docker 29.5.3 disponível; PostgreSQL do Compose preservado e saudável em loopback.

## Plano de execução

1. Criar migration incremental para usuários, recuperação e Spring Session JDBC.
2. Implementar persistência, DTOs, serviços, endpoints e autorização.
3. Integrar sessão persistida, rotação, logout e CSRF.
4. Implementar recuperação com capturador de e-mail em teste e Mailpit local.
5. Implementar provisionamento ADMIN dedicado e limitação de tentativas.
6. Executar testes PostgreSQL/MVC/security e smoke HTTP.
7. Atualizar documentação, revisar diff e criar commit local.

## Decisões tomadas

- Spring Session JDBC foi escolhido para manter sessões opacas persistidas no PostgreSQL, com `@EnableJdbcHttpSession`; o schema oficial é criado pelo Flyway, sem inicialização concorrente.
- O login usa `DaoAuthenticationProvider`, BCrypt custo 12, troca do identificador de sessão e salvamento explícito do `SecurityContext`.
- O e-mail é normalizado com trim e minúsculas `Locale.ROOT`; senha não é trimada nem normalizada e aceita 12–72 caracteres, até 72 bytes UTF-8.
- Cadastro público cria somente `CUSTOMER`, sem login automático. O primeiro ADMIN é provisionado por runner dedicado, desabilitado por padrão, idempotente e sem promoção silenciosa.
- Tokens de recuperação são aleatórios, somente o SHA-256 é persistido, o consumo é atômico e a redefinição invalida sessões anteriores.
- O limitador é local à instância, com `429`/`Retry-After`; a limitação distribuída permanece pendência de produção.

## Alterações por funcionalidade

### Persistência e migração

- `backend/src/main/resources/db/migration/V2__create_identity_and_session_tables.sql`: tabelas `app_users`, `password_reset_tokens`, `spring_session` e `spring_session_attributes`, com FKs, unicidade e checks.
- `backend/src/main/java/com/perfumestore/backend/user/`: usuário, papel, estado, normalização e repository JDBC.
- `backend/src/main/java/com/perfumestore/backend/auth/SessionInvalidationRepository.java`: revogação das sessões do usuário por principal UUID.

### Sessão, segurança e autenticação

- `SessionConfiguration`, `SecurityConfig` e propriedades de cookie configuram sessão JDBC, timeout, SameSite, Secure por ambiente, rota pública mínima e rotas privadas por padrão.
- `AuthenticationService` implementa login, rotação, persistência explícita de contexto, renovação de CSRF e logout idempotente.
- `AuthController` implementa cadastro, login, logout, `/me`, atualização limitada de nome, solicitação de recuperação e redefinição.

### Recuperação, e-mail e proteção contra abuso

- `PasswordRecoveryService`, `PasswordResetTokenRepository`, `TokenSecurity` e `PasswordPolicy` implementam token de uso único, expiração, invalidação de sessões e política de senha.
- `SmtpEmailSender` usa SMTP configurado; Mailpit é o adaptador local do Compose; `CapturedEmailSender` é exclusivo dos testes.
- `AttemptRateLimiter` aplica limites configuráveis e evita crescimento indefinido com limpeza limitada.

### Provisionamento administrativo

- `AdminProvisioningRunner` e `AdminProvisioningService` executam somente com propriedade explícita, usando o perfil `provision-admin` sem servidor web.
- `.env.example` contém apenas nomes de variáveis vazios/ilustrativos; nenhuma senha real foi gravada.

### Testes e documentação

- Foram adicionados testes MockMvc, Spring Security, PostgreSQL Testcontainers, concorrência de token e HTTP real com sessão JDBC.
- Roadmaps foram preservados; contratos, arquitetura, decisões, plano de validação, README e status receberam a especificação da Fase 3.
- Os logs brutos ficam somente em `Docs/execucoes/logs/`, ignorados pelo Git.

## Problemas encontrados e correções

- A primeira execução dos testes revelou que `MockMvc` não representava corretamente a rotação/persistência JDBC ao reutilizar uma `MockHttpSession`; a cobertura foi separada: fixtures de identidade usam `user(...)` e o teste HTTP real verifica cookie, persistência e rotação.
- O teste de logout passou uma sessão nula quando o login MockMvc criava o contexto pelo cookie; o helper passou a fornecer uma sessão inicial explícita.
- Dados de sessões de testes anteriores contaminavam a asserção de invalidação; o teardown passou a limpar tabelas de teste em ordem referencial.
- A primeira prova HTTP encontrou zero sessões porque o starter JDBC não estava presente como auto-configuração suficiente para o filtro neste arranjo; `@EnableJdbcHttpSession` foi adicionado e a prova passou.
- Uma captura inicial de log usou caminho relativo incorreto; os comandos seguintes passaram a gravar em `..\Docs\execucoes\logs` a partir de `backend`.

## Verificações e resultados

- Testes direcionados finais: 10 em `AuthenticationIntegrationTest`, 2 em `AdminProvisioningIntegrationTest` e 1 em `SessionPersistenceIntegrationTest`, todos aprovados.
- A prova HTTP real confirmou PostgreSQL, Flyway V2, cookie de sessão, acesso autenticado posterior, e-mail capturado, redefinição, remoção da sessão antiga e rejeição da senha anterior.
- `clean verify` e smoke local com PostgreSQL/Mailpit foram executados e estão registrados abaixo e em `FASE_03_TESTES.md`.

## Pendências e limitações

- Mailpit é somente desenvolvimento; ainda falta provedor transacional, domínio/remetente, retry/outbox e observabilidade de produção.
- O limitador de tentativas é local à instância e não é suficiente para múltiplas réplicas.
- Não há MFA, verificação de e-mail, troca de e-mail/senha por fluxo separado, catálogo, endereços comerciais ou frontend nesta fase.
- O provisionamento ADMIN exige execução operacional dedicada e credenciais fornecidas fora do repositório.

## Estado final

Implementação da Fase 3 concluída localmente após `clean verify`, smoke HTTP completo, revisão documental e commit local. A conclusão cobre autenticação, usuários, sessões persistidas, perfil, recuperação e provisionamento descritos nesta fase; não inicia a Fase 4.
## Atualização final da execução

- `clean verify` executado com Java 17.0.16: **BUILD SUCCESS**, 27 testes, 0 falhas, 0 erros, 0 ignorados; JAR em `backend/target/perfumestore-backend-0.0.1-SNAPSHOT.jar`.
- Smoke local executado com PostgreSQL e Mailpit: `csrf=200`, `register=201`, `login=200`, `me=200`, `profile=200`, `logout=204`, `oldCookie=401`, `recovery=202`, `reset=204`, `oldPassword=401`, `newPassword=200`.
- Histórico do banco local: Flyway Schema Creation, V1 e V2 com `success=true`; nenhum volume foi removido.
- Mailpit foi parado após o smoke; PostgreSQL permaneceu preservado sem `down -v`.
- Os três documentos de execução foram revisados para não conter credenciais, tokens, cookies ou links reais.
