# Fase 4 — comandos e resultados

Registro sanitizado e reproduzível. Senhas, tokens, cookies, e-mails reais, URLs de recuperação e credenciais foram omitidos. Saídas longas foram gravadas em `Docs/execucoes/logs/`, ignorado pelo Git.

| Ordem | Diretório | Comando sanitizado | Finalidade | Código | Resultado relevante |
|---:|---|---|---|---:|---|
| 1 | raiz | `Get-Location; git status; git log -1` | Confirmar diretório, HEAD e working tree | 0 | HEAD Fase 3; árvore inicial limpa. |
| 2 | raiz/backend | `java -version; .\mvnw.cmd --version; docker version` | Confirmar JDK, Wrapper e Docker | 0 | Java 17.0.16, Maven 3.9.16, Docker 29.5.3. |
| 3 | raiz | leitura de roadmaps, status, contratos, README, migrations, segurança e testes | Revisar contexto e integração futura | 0 | Documentação da Fase 3 e frontend consultadas. |
| 4 | raiz/backend | `mvnw.cmd -DskipTests compile` | Compilação inicial após base de catálogo | 0 | 73 fontes compiladas. |
| 5 | raiz/backend | `mvnw.cmd clean verify` | Regressão antes do teste novo | 0 | 27 testes, 0 falhas, 0 erros; JAR gerado. Log: `phase4-current-verify.log`. |
| 6 | raiz/backend | `mvnw.cmd -DskipTests test-compile` | Compilar teste de catálogo | 1 e depois 0 | Corrigidos text blocks e import; compilação final passou. Logs `phase4-test-compile*.log`. |
| 7 | raiz/backend | `mvnw.cmd -Dtest=CatalogIntegrationTest test` | Executar integração de catálogo | 0 | 4 testes, 0 falhas, 0 erros, PostgreSQL 18.6 via Testcontainers. Log: `phase4-catalog-tests-2.log`. |
| 8 | raiz | `docker compose up -d postgres` | Preparar PostgreSQL persistente sem remover volume | 0 | Container saudável em loopback. |
| 9 | raiz/backend | `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local` | Iniciar aplicação para smoke | 0 | Java 17, Flyway validou V1–V3 e Tomcat iniciou em 8080. |
| 10 | raiz | `docker compose up -d mailpit` com portas locais configuradas | Disponibilizar dependência SMTP da configuração local | 0 | Mailpit iniciado em loopback; health passou a `200`. |
| 11 | raiz/backend | `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local,provision-admin` com credenciais geradas apenas na sessão | Revisar provisionamento dedicado | 1 antes da correção, 1 após recusa esperada | Primeiro defeito foi corrigido; depois a regra recusou novo ADMIN porque já havia ADMIN ativo, sem promover ou redefinir conta. Logs locais ignorados. |
| 12 | raiz | consulta sanitizada de `flyway_schema_history` e contagem de tabelas | Confirmar V3 e ausência de estoque/carrinho/pedidos | 0 | V3 registrada; quatro tabelas de catálogo, sem tabelas dos domínios posteriores. |
| 13 | raiz | `git diff --check; git status; git diff --stat` | Revisar whitespace, escopo e arquivos | 0 | Executado após `clean verify`; revisão final antes do staging. |
| 14 | raiz | `git add ...; git commit -m "feat: implement catalog categories presentations and images"` | Criar commit local | 0 | Commit local criado; hash confirmado no relatório final. |

Os comandos de smoke que poderiam conter credenciais foram executados em script temporário ignorado, com valores gerados em memória; não foram copiados para este registro nem para logs versionados.
