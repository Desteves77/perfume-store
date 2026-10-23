# Fase 7 — Testes e evidências

## Estado inicial

A referência histórica da Fase 6 é de 49 testes aprovados. Esse número será confirmado novamente pela execução da suíte e não será tratado como resultado atual.

## Registro contínuo

Serão registrados testes focados, PostgreSQL/Testcontainers, concorrência, merge, segurança, expiração, smoke HTTP, Flyway, build, persistência e regressão, com comandos, contagens, falhas e reexecuções reais.

## Resultados executados

### Testes automatizados

| Grupo | Comando | Total | Aprovados | Falhas | Erros | Ignorados | Evidencia |
|---|---|---:|---:|---:|---:|---:|---|
| Foco cart | `backend\\mvnw.cmd --batch-mode -Dtest=CartIntegrationTest test` | 9 | 9 | 0 | 0 | 0 | PostgreSQL/Testcontainers real |
| Regressao catalogo | `backend\\mvnw.cmd --batch-mode -Dtest=CatalogIntegrationTest test` | 6 | 6 | 0 | 0 | 0 | V3-V6 e publicacao |
| Suite completa | `backend\\mvnw.cmd --batch-mode clean verify` | 58 | 58 | 0 | 0 | 0 | JAR executavel gerado |

As classes existentes de infraestrutura, contrato, autenticacao, sessao, endereco/frete e estoque foram executadas na suite completa. A contagem final real foi 58, acima dos 49 testes da referencia da Fase 6.

### Cenarios de cart cobertos

- Visitante sem cookie recebe resposta vazia sem criar registro; a primeira inclusao cria carrinho e cookie opaco.
- Cookie e token sao persistentes entre requisicoes; a API nao retorna o token.
- Inclusao idempotente repetida nao duplica; chave com fingerprint diferente retorna conflito.
- Inclusao concorrente da mesma apresentacao preserva uma linha e soma o efeito esperado; duas associacoes concorrentes transferem o visitante somente uma vez.
- PATCH absoluto usa versao; DELETE e repetivel; apresentacao inativa nao entra.
- Preco alterado gera `PRICE_CHANGED`; saldo reduzido gera `OUT_OF_STOCK`/`QUANTITY_EXCEEDS_AVAILABLE`; nenhuma reserva e criada.
- Login real combina carrinho visitante com carrinho preexistente da conta; repeticao nao duplica; logout preserva e outro cliente nao acessa.
- CSRF e isolamento por cliente foram verificados no teste de integracao.
- TTL, estado expirado, retencao e limpeza em lote foram verificados sem tocar catalogo ou estoque.

### Smoke HTTP isolado

Comando: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File Docs/execucoes/logs/phase7-http-smoke.ps1`.

O primeiro roteiro falhou em uma verificacao de cookie por consultar o caminho raiz do jar; apos a correcao, a segunda execucao encontrou a necessidade de resetar o preco da fixture persistente; a terceira execucao foi aprovada: `SMOKE_RESULTS total=18 failed=0`. Os 18 cenarios incluem health, carrinho anonimo, cookie HttpOnly, persistencia, duas apresentacoes, PATCH, DELETE, subtotal, cadastro, login/merge, logout, novo login, isolamento, revalidacao de preco/estoque e token invalido.

### Persistencia e migrations

- O JAR foi parado e iniciado novamente; login e carrinho do cliente sintetico foram recuperados.
- O PostgreSQL isolado foi parado e iniciado sem remover volume; o segundo check retornou `login=200 cartItems=1 smokeItem=1 flywayRows=7 counts=12,9,0`.
- Flyway validou V1-V6 e informou schema atualizado sem reaplicar migrations.
- Consulta do estoque retornou `reserved_quantity=0` para as apresentacoes e `inventory_movements=0`; o carrinho nao reservou nem movimentou estoque.
- OpenAPI respondeu HTTP 200 e publicou os quatro caminhos de cart.

### Relatorios Maven

Os relatorios individuais ficam em `backend/target/surefire-reports/`. Logs completos dos comandos ficam em `Docs/execucoes/logs/phase7-*.log`, ignorados pelo Git. Nao houve testes ignorados e nao foram desabilitados testes para obter build verde. O `clean verify` exibiu avisos de encerramento do job de limpeza do Spring Session enquanto containers Testcontainers eram desmontados; isso nao produziu falha, erro ou teste ignorado.
