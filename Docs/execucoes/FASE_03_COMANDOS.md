# Fase 3 — comandos e resultados

Registro técnico sanitizado. Senhas, tokens, cookies, links de recuperação, strings de conexão e credenciais não são registrados. Os logs de build/teste ficam em `Docs/execucoes/logs/`, ignorados pelo Git.

| Ordem | Diretório | Comando sanitizado | Finalidade | Código | Resultado |
|---:|---|---|---|---:|---|
| 1 | raiz | `Get-Location; git status; git log -1` | Confirmar diretório, HEAD e working tree | 0 | HEAD Fase 2; sem alterações iniciais; sem `AGENTS.md`. |
| 2 | `backend` | `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'; .\mvnw.cmd --version` | Confirmar JDK e Wrapper | 0 | Java 17.0.16; Maven Wrapper 3.9.16. |
| 3 | raiz | `docker info; docker compose ps` | Confirmar Docker/PostgreSQL | 0 | Docker 29.5.3; PostgreSQL saudável em loopback. |
| 4 | `backend` | `mvnw ... -Dtest=InfrastructureIntegrationTest,ApiContractIntegrationTest test` | Regressão da Fase 2 após V2 | 0 | 14 testes, 0 falhas, 0 erros. Log: `fase03-regressao-base.log`. |
| 5 | `backend` | `mvnw ... -Dtest=... test` | Primeira execução dos novos testes | 1 | Problemas de fixture MockMvc e filtro JDBC identificados; log ignorado. |
| 6 | `backend` | `mvnw ... -Dtest=AuthenticationIntegrationTest test` | Repetir autenticação após correções | 0 | 10 testes, 0 falhas, 0 erros. Log: `fase03-auth-tests-5.log`. |
| 7 | `backend` | `mvnw ... -Dtest=SessionPersistenceIntegrationTest test` | Sessão JDBC em HTTP real | 0 | 1 teste, 0 falhas, 0 erros. Log: `fase03-session-http-2.log`. |
| 8 | `backend` | `mvnw ... -Dtest=AuthenticationIntegrationTest,AdminProvisioningIntegrationTest,SessionPersistenceIntegrationTest test` | Suíte direcionada | 0 | 12 testes, 0 falhas, 0 erros antes da inclusão do cenário de falha de e-mail; a execução consolidada final está no `clean verify` com 27 testes. |
| 9 | raiz | `docker compose config --quiet` | Validar Compose com Mailpit | 0 | Sintaxe e interpolação válidas. |
| 10 | raiz | `docker compose up -d postgres mailpit` | Iniciar banco e SMTP local sem apagar volume | 0 | PostgreSQL e Mailpit iniciados; portas em loopback. |
| 11 | `backend` | `mvnw ... clean verify 2>&1 | Tee-Object ..\Docs\execucoes\logs\fase03-clean-verify.log` | Build, testes, migrations e empacotamento | 0 | `BUILD SUCCESS`; JAR executável gerado. |
| 12 | `backend` | `& ..\Docs\execucoes\logs\start-smoke.ps1`; `& ..\Docs\execucoes\logs\run-smoke.ps1` | Iniciar JAR local e executar smoke HTTP com PostgreSQL/Mailpit | 0 | Health, CSRF, cadastro, login, perfil, logout, recuperação e redefinição exercitados sem imprimir dados sensíveis. Log: `fase03-smoke.log`. |
| 13 | raiz | `docker compose exec postgres psql ... select ... flyway_schema_history` | Confirmar migrations | 0 | V1 e V2 com `success=true`, sem alteração destrutiva. |
| 14 | raiz | `git diff --check; git status; git diff --cached --stat` | Revisar diff e staging | 0 | Somente arquivos da Fase 3, sem `.env`, `target` ou logs. |
| 15 | raiz | `git commit -m "feat: implement users authentication and password recovery"` | Registrar entrega local | 0 | Commit local criado; sem push/deploy. |

Os códigos e resultados desta tabela correspondem à execução final documentada no relatório de testes; logs brutos permanecem ignorados.
