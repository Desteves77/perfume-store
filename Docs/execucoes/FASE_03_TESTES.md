# Fase 3 — testes e evidências

Os registros abaixo não contêm tokens, senhas, cookies, links reais de recuperação ou credenciais. Relatórios detalhados do Maven ficam em `backend/target/surefire-reports/` e não são versionados.

## Resumo final

| Suíte | Testes | Falhas | Erros | Ignorados | Evidência |
|---|---:|---:|---:|---:|---|
| `InfrastructureIntegrationTest` | 2 | 0 | 0 | 0 | `backend/target/surefire-reports/TEST-com.perfumestore.backend.InfrastructureIntegrationTest.xml` |
| `ApiContractIntegrationTest` | 12 | 0 | 0 | 0 | `backend/target/surefire-reports/TEST-com.perfumestore.backend.ApiContractIntegrationTest.xml` |
| `AuthenticationIntegrationTest` | 10 | 0 | 0 | 0 | `backend/target/surefire-reports/TEST-com.perfumestore.backend.AuthenticationIntegrationTest.xml` |
| `AdminProvisioningIntegrationTest` | 2 | 0 | 0 | 0 | `backend/target/surefire-reports/TEST-com.perfumestore.backend.AdminProvisioningIntegrationTest.xml` |
| `SessionPersistenceIntegrationTest` | 1 | 0 | 0 | 0 | `backend/target/surefire-reports/TEST-com.perfumestore.backend.SessionPersistenceIntegrationTest.xml` |
| **Total Fase 1–3** | **27** | **0** | **0** | **0** | `clean verify` |

## Cenários executados

| ID | Cenário | Teste/comando | Resultado esperado e observado |
|---|---|---|---|
| F3-001 | Cadastro válido | `registersCustomerWithHashedPasswordAndCannotAssignAdmin` | `201`, `CUSTOMER`, sem hash/senha na resposta; aprovado. |
| F3-002 | Validação e duplicidade normalizada | `normalizesEmailAndRejectsConcurrentStyleDuplicateByDatabaseConstraint` | `409 EMAIL_ALREADY_REGISTERED`; aprovado. |
| F3-003 | Login válido, CSRF e perfil | `loginPersistsSessionRotatesIdentifierAndSupportsAuthenticatedProfile` | `200`, sessão persistida e perfil sem escalada; aprovado. |
| F3-004 | Login inválido e limite | `invalidLoginIsGenericAndExcessiveAttemptsReturn429` | mensagem genérica; excesso `429` com `Retry-After`; aprovado. |
| F3-005 | Logout e repetição | `logoutRevokesPersistedSessionAndCanBeRepeated` | sessão revogada, `401` posterior e segundo logout seguro; aprovado. |
| F3-006 | Recuperação genérica | `recoveryResponseIsGenericAndResetUsesOneTimeToken` e `recoveryDoesNotRevealUnknownEmailAndExpiredTokenIsRejected` | desconhecido/expirado sem enumeração; aprovado. |
| F3-007 | Token hash, uso único e senha | `recoveryResponseIsGenericAndResetUsesOneTimeToken` | hash no banco, token reutilizado inválido, senha antiga falha e nova funciona; aprovado. |
| F3-008 | Falha de e-mail | `mailFailureKeepsRecoveryResponseGeneric` | `202` genérico e token removido; aprovado. |
| F3-009 | Consumo concorrente | `sameResetTokenCanOnlyBeConsumedOnceUnderConcurrency` | exatamente um sucesso; aprovado com PostgreSQL. |
| F3-010 | Sessão HTTP real | `realHttpSessionIsPersistedRotatedAndRevoked` | cookie rotacionado, registro JDBC, reset remove sessão, nova senha funciona; aprovado. |
| F3-011 | Provisionamento ADMIN | `AdminProvisioningIntegrationTest` | cria uma vez, reexecução idempotente e não promove CUSTOMER; aprovado. |
| F3-012 | Regressão Fase 1/2 | `clean verify` | migrations, health, API, CSRF, CORS, OpenAPI e build preservados; aprovado. |

## Smoke local

Com PostgreSQL do Compose e Mailpit local, perfil `local`, a sequência foi: obter CSRF; cadastrar cliente; autenticar; consultar e atualizar `/api/v1/auth/me`; fazer logout; confirmar rejeição do cookie antigo; solicitar recuperação; localizar a mensagem no Mailpit; extrair o token somente em memória do processo de teste; redefinir a senha; autenticar com a senha nova. A saída compartilhável registra apenas códigos HTTP e estados, nunca valores de credenciais.

## Relatórios Maven

Os relatórios ficam em `backend/target/surefire-reports/` após `clean verify`. Logs de execução ficam em `Docs/execucoes/logs/`, ignorados por `.gitignore`. O workflow do GitHub foi criado nas fases anteriores e não é declarado como executado no GitHub nesta entrega.
