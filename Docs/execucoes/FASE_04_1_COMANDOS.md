# Fase 4.1 — Comandos sanitizados

Este arquivo registra comandos executados e resultados sem credenciais, tokens, cookies ou strings sensíveis.

| Ordem | Diretório | Comando sanitizado | Finalidade | Saída | Resultado |
|---:|---|---|---|---:|---|
| 1 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `git rev-parse HEAD; git status --short --branch` | Registrar estado inicial | 0 | HEAD `cde8080`; árvore limpa |
| 2 | `C:\Users\User\Desktop\Ecommerce PerfumeStore` | `docker compose -p perfumestore-catalog-acceptance -f compose.catalog-acceptance.yaml --env-file .env.catalog-acceptance config --quiet` | Validar Compose isolado | 0 | configuração válida |

Credenciais sintéticas e variáveis locais são geradas/carregadas sem imprimir seus valores. Saídas extensas de Maven, Docker e smoke serão gravadas em `Docs/execucoes/logs/`.

## Registro complementar

| Ordem | Diretório | Comando sanitizado | Finalidade | Código | Resultado relevante |
|---:|---|---|---|---:|---|
| 3 | raiz | `docker compose ... up -d postgres mailpit` | Iniciar ambiente isolado | 1 | `55432` recusada pelo Windows; nenhum banco iniciou |
| 4 | raiz | `docker compose ... down` | Remover containers parciais do próprio projeto | 0 | rede/containers removidos; volume preservado |
| 5 | raiz | `docker compose ... up -d postgres mailpit` | Iniciar com porta `56000` | 0 | PostgreSQL e Mailpit ativos; ambos saudáveis |
| 6 | backend | `mvnw.cmd --batch-mode --no-transfer-progress spring-boot:run -Dspring-boot.run.profiles=local,provision-admin` | Provisionar ADMIN sintético | 0 | processo encerrou; nenhum segredo no log |
| 7 | backend | mesmo comando dedicado | Verificar idempotência | 0 | contagem de ADMIN permaneceu `1` |
| 8 | backend | `mvnw.cmd ... spring-boot:run -Dspring-boot.run.profiles=local` | Iniciar aplicação em `18080` | 0 | health HTTP `200` |
| 9 | raiz | `powershell -NoProfile -File phase4-1-smoke.ps1` | Executar smoke HTTP completo | 0 | todos os cenários planejados aprovados |
| 10 | raiz | `docker compose ... restart postgres` | Reiniciar o banco isolado | 0 | container dedicado voltou a `healthy` |
| 11 | raiz | `powershell -NoProfile -File phase4-1-persistence-http.ps1` | Verificar dados e arquivos após reinício | 0 | estados, Flyway, acesso e checksums preservados |
| 12 | backend | `mvnw.cmd --batch-mode --no-transfer-progress -Dtest=CatalogIntegrationTest test` | Executar suíte de catálogo | 0 | 6 testes, 0 falhas, 0 erros, 0 ignorados |
| 13 | backend | `mvnw.cmd --batch-mode --no-transfer-progress clean verify` | Verificação final e JAR | 0 | 33 testes, 0 falhas, 0 erros, 0 ignorados; BUILD SUCCESS |
| 14 | raiz | `Stop-Process` somente nos PIDs da aplicação; `docker compose ... stop` | Encerrar recursos desta execução | 0 | aplicação e containers de homologação parados; dados preservados |

As saídas longas correspondentes estão em `Docs/execucoes/logs/phase4-1-*.log`, todos ignorados pelo Git. Comandos envolvendo `.env.catalog-acceptance` foram registrados sem seus valores.
