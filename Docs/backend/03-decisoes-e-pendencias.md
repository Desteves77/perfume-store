# PerfumeStore — Fase 0: decisões e pendências

## 1. Decisões já estabelecidas

Estas decisões vêm da solicitação e dos roadmaps e não devem ser reabertas como escolhas de implementação rotineiras:

- backend em Java/Spring Boot, PostgreSQL, Maven, Flyway, Spring Security e API REST com OpenAPI;
- monólito modular por domínio;
- valores monetários com precisão decimal e configurações/segredos externos ao código;
- uma loja, BRL, perfis `CUSTOMER` e `ADMIN`, preço e estoque por apresentação;
- um provedor de pagamento, checkout hospedado e operação administrativa manual quando necessário;
- backend como autoridade para validações, autenticação, autorização, preços, frete, estoque, pedidos, pagamentos e transições;
- sequência: backend comercial básico, frontend, integrações/refinamento/homologação e liberação do MVP;
- exclusão de cupons, marketplace, fidelidade, avaliações, favoritos, ERP, fiscal automático e relatórios avançados;
- nenhuma implementação de aplicação, migration, endpoint ou frontend nesta Fase 0.

## 2. Recomendações técnicas desta fase

| ID | Recomendação | Motivo | Fase principal |
|---|---|---|---|
| REC-01 | Sessão opaca no servidor em cookie seguro, com sessão persistida e revogável | logout efetivo, revogação simples e menor complexidade que JWT | Fase 3 |
| REC-02 | Cookie de visitante com token aleatório; banco guarda hash | carrinho persistido sem confiar em ID escolhido pelo navegador | Fase 7 |
| REC-03 | Carrinho não reserva estoque; merge soma linhas e bloqueia conflitos sem descarte | preserva intenção do cliente e evita redução silenciosa | Fase 7 |
| REC-04 | Reserva na criação do pedido, baixa física na aprovação válida e conversão da reserva | separa disponibilidade temporária de venda confirmada | Fases 5 e 8 |
| REC-05 | `Idempotency-Key` com fingerprint por usuário/rota para pedido e pagamento | reenvio seguro após timeout e prevenção de duplicidade | Fase 8 |
| REC-06 | Webhook por evento único, assinatura validada e reconciliação de estados desconhecidos | tolera repetição, ordem variável e falhas de rede | Fase 9 |
| REC-07 | Aprovação tardia tenta nova reserva; sem estoque vai para `PAYMENT_REVIEW` e não entrega | impede venda acima da disponibilidade | Fase 9 |
| REC-08 | Snapshot de itens, totais, endereço e frete no pedido | histórico não muda com catálogo ou endereço atual | Fase 8 |
| REC-09 | Cancelamento, reembolso e devolução física são registros separados | dinheiro e produto percorrem processos diferentes | Fase 10 |
| REC-10 | Outbox de notificações com chave de evento e retries | e-mail não desfaz venda e não duplica por reprocessamento | Fase 11 |
| REC-11 | Configuração de frete por área/regra versionada no backend | mantém regra comercial fora do frontend e permite alteração controlada | Fase 6 |
| REC-12 | `audit_logs` append-only para ações administrativas e transições sensíveis | rastreabilidade operacional sem registrar segredos | Fases 5, 8 e 10 |

Estas são recomendações, não decisões comerciais do proprietário. A implementação deve registrar qualquer alteração no documento de decisões e nos contratos afetados.

## 3. Configurações provisórias exclusivas de desenvolvimento

Podem existir somente com guarda de ambiente e sem acesso em produção:

- provedor de pagamento simulado;
- e-mail em caixa local, logger sanitizado ou servidor de testes;
- armazenamento local de imagens;
- valores/áreas de frete fictícios para testes automatizados, identificados como dados de teste;
- relógio controlável e dados seed de catálogo/estoque;
- criação de administrador de desenvolvimento por variável/CLI sem credencial versionada.

Essas configurações não são decisões de operação nem podem aparecer como dados reais no frontend ou na documentação pública.

## 4. Pendências externas e comerciais

| Pendência | Sugestão inicial | Impacto | Bloqueia |
|---|---|---|---|
| Provedor de pagamento | escolher um único provedor com checkout hospedado, webhooks assinados e consulta de reconciliação | define SDK/adaptador, estados, assinatura, refund e sandbox | não bloqueia Fases 1–8; bloqueia homologação da Fase 9 |
| Serviço de e-mail | provedor transacional com sandbox e retry | define templates, remetente, limites e observabilidade | não bloqueia Fases 1–8; bloqueia operação da Fase 11 |
| Armazenamento de imagens | objeto/CDN persistente com URLs controladas | define upload, limites, remoção e cache | não bloqueia modelo; bloqueia integração completa da Fase 4 |
| Hospedagem e domínios | confirmar frontend/API, HTTPS, proxy e domínio registrável comum | altera cookies, CORS, CSRF e callbacks | não bloqueia código inicial; bloqueia homologação real das Fases 12–13 |
| Área atendida | definir cidades/UF/faixas de CEP e exceções | define validação de destino | não bloqueia Fases 1–5; bloqueia frete real na Fase 6 |
| Regra, valores e prazos de frete | aprovar modalidade, tabela e prazo exibido | define total comercial e snapshot do pedido | não bloqueia contrato/protótipo; bloqueia aceite da Fase 6 |
| Prazo de reserva | aprovar duração operacional e política de limpeza | afeta conversão, expiração e pagamentos tardios | não bloqueia Fases 1–7; bloqueia parametrização final da Fase 8/9 |
| Cancelamento e reembolso | documentar responsáveis, SLA, critérios e uso do painel do provedor | define ações ADMIN e mensagens ao cliente | não bloqueia checkout simulado; bloqueia operação da Fase 10 |
| Primeiro administrador | escolher procedimento de provisionamento e responsável | afeta bootstrap seguro da operação | não bloqueia desenvolvimento; bloqueia acesso administrativo de homologação |
| Dados reais da loja | nome, contato, políticas e identidade visual | afeta frontend e publicação | não bloqueia backend; bloqueia lançamento integrado |

Nenhuma conta, serviço ou contratação será criada nesta fase. Onde a decisão externa ainda falta, a documentação usa identificadores genéricos e declara a pendência.

## 5. Pendências de implementação controlada

- Fixar versões e matriz de compatibilidade na Fase 1 após verificação técnica.
- Definir limites de página, upload, tamanho de carrinho, expiração de sessão e retenção de logs.
- Escolher estratégia concreta de busca por nome depois de medir o catálogo inicial.
- Definir contrato OpenAPI executável a partir destes contratos iniciais.
- Definir política de retenção e anonimização de dados pessoais conforme orientação jurídica/operacional.
- Confirmar se o deploy usará um ou mais nós e, portanto, se sessão compartilhada exigirá armazenamento centralizado.

## 6. Conflitos identificados

Não foi encontrado conflito relevante entre os dois roadmaps e o escopo desta fase. Ambos mantêm as regras no backend, tratam pagamento por confirmação do servidor, exigem idempotência no checkout e deixam a escolha de serviços externos para etapas posteriores. Os documentos de roadmap permanecem inalterados.

