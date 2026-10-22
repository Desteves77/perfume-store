# PerfumeStore — Fase 0: escopo e regras de negócio

Status: especificação inicial para orientar a Fase 1. Este documento não representa funcionalidades implementadas.

Fontes consultadas: [roadmap do backend](../ROADMAP_BACKEND.md) e [roadmap do frontend](../ROADMAP_FRONTEND.md). O backend continua sendo a autoridade para validações, preços, disponibilidade, frete, totais, transições de estado e permissões.

## 1. Escopo do MVP

O MVP cobre uma única loja, em português, com BRL como moeda, os perfis `CUSTOMER` e `ADMIN`, preço e estoque por apresentação comercial, um provedor de pagamento com checkout hospedado, uma regra inicial de frete e operação administrativa manual para o que o provedor ou a transportadora não automatizarem.

Inclui:

- catálogo público paginado, busca por nome, categoria, detalhes, imagens e apresentações;
- cadastro, login, logout, recuperação de acesso e dados pessoais permitidos;
- endereços do cliente;
- carrinho persistido para visitante e cliente;
- cotação de frete, criação de pedido, pagamento hospedado e acompanhamento;
- estoque físico, reservado e disponível por apresentação;
- administração de categorias, produtos, apresentações, imagens, estoque e pedidos;
- separação, envio, rastreamento, entrega, cancelamentos, solicitações de reembolso e devoluções;
- notificações transacionais pendentes e reprocessáveis;
- histórico de ações administrativas relevantes.

Ficam fora do MVP: cupons, marketplace, múltiplas lojas ou moedas, fidelidade, avaliações, favoritos, ERP, integração fiscal automática, relatórios avançados, cálculo de frete contratado com múltiplos transportadores e formulário próprio para dados de cartão.

## 2. Vocabulário e invariantes

- **Produto**: item comercial e editorial, como um perfume de determinada marca.
- **Apresentação**: forma vendável do produto, com SKU, volume, preço, ativação e estoque próprios. Exemplos de volumes são dados de catálogo; não são regras comerciais.
- **Disponível**: `estoque_fisico - estoque_reservado`. A API pública expõe apenas a disponibilidade necessária à compra.
- **Reserva**: alocação temporária vinculada a um pedido e a uma apresentação. Adicionar ao carrinho nunca cria reserva.
- **Pedido**: registro imutável dos dados comerciais aceitos no momento da criação, com estados próprios para pagamento e entrega.
- **Reembolso**: operação financeira registrada separadamente da devolução física e só considerada concluída após confirmação do provedor ou do procedimento manual definido.

As seguintes invariantes valem para todos os módulos:

1. O servidor calcula preço de linha, subtotal, frete e total usando dados persistidos e configuração vigente.
2. Uma apresentação inativa ou um produto inativo não pode ser comprado. Itens já presentes no carrinho permanecem visíveis com pendência para que o cliente resolva explicitamente.
3. O pedido guarda cópia do SKU, nome, marca, volume, preço, quantidade, totais e endereço. Alterar o catálogo ou o endereço depois da compra não altera o histórico.
4. Toda operação que movimenta estoque é transacional, auditável e idempotente por referência de negócio.
5. Cliente só acessa seus próprios dados, carrinhos e pedidos. `ADMIN` recebe autorização adicional por endpoint e por ação.
6. Nenhum retorno do navegador é prova de pagamento. A confirmação depende de webhook válido ou reconciliação autenticada com o provedor.
7. Falha de e-mail não desfaz pedido, estoque ou pagamento; a notificação fica pendente para nova tentativa.

## 3. Produtos, apresentações e imagens

O produto concentra identificador público, slug, nome, marca, descrição, categoria, estado de publicação e timestamps. A apresentação concentra SKU único, volume e unidade, nome de exibição opcional, preço em BRL, estado ativo e timestamps. O estoque pertence à apresentação.

Imagens pertencem ao produto, possuem chave de armazenamento, URL derivada, texto alternativo, posição e indicação de principal. O armazenamento deve ser persistente e externo à aplicação quando a decisão de serviço for tomada. Remoção é lógica (`deleted_at` ou estado inativo) quando houver referência comercial, e uma imagem não pode ser apagada fisicamente enquanto uma política de retenção ou auditoria exigir preservação. A API só publica imagens ativas e autorizadas.

Produto e apresentação são desativados logicamente. Desativar o produto torna todas as apresentações indisponíveis para novas compras; a implementação deve revalidar os dois níveis. Não há exclusão física de produto, apresentação ou categoria referenciada por pedido. Ajustes editoriais futuros não reescrevem `order_items`.

## 4. Carrinho

O carrinho é persistido no servidor. Para visitante, a aplicação cria um valor aleatório opaco de alta entropia, enviado em cookie `HttpOnly`, `Secure` e com política `SameSite` compatível com a topologia aprovada. O banco armazena somente o hash do token. O cliente não escolhe nem envia um identificador de carrinho como autoridade. Para cliente autenticado, o carrinho é associado ao usuário.

Adicionar, alterar ou remover item recalcula o carrinho no servidor. A resposta informa, por item, estado de disponibilidade, preço vigente, preço anterior quando detectado, quantidade solicitada e ações permitidas. Quantidade é inteira positiva e limitada por uma configuração técnica de segurança; ela não é reduzida silenciosamente por estoque.

Itens inativos, esgotados, com quantidade acima da disponibilidade ou com preço alterado continuam armazenados e marcados com problema. Uma tentativa de checkout é rejeitada até que os conflitos sejam resolvidos. Uma tentativa de adicionar ou aumentar quantidade além do limite técnico retorna erro explícito e não altera a quantidade anterior.

O carrinho do visitante não reserva estoque. A expiração é configurável por ambiente, com sugestão inicial de 30 dias após a última atividade; o valor não é decisão comercial. Um processo posterior pode marcar carrinhos abandonados como expirados, sem apagar imediatamente o histórico operacional necessário.

Ao autenticar, o backend tenta associar o carrinho do visitante ao carrinho ativo do usuário. A regra é determinística:

1. linhas da mesma apresentação são somadas;
2. linhas diferentes são preservadas;
3. estados de disponibilidade não são apagados na associação;
4. se a soma ultrapassar o limite técnico da linha, a operação retorna `409 CART_MERGE_REQUIRES_REVIEW`, não altera nenhum dos dois carrinhos e lista as linhas conflitantes;
5. estoque insuficiente não bloqueia a associação, mas fica sinalizado para resolução antes do checkout;
6. após associação bem-sucedida, o carrinho de visitante é marcado como `MERGED` e o carrinho do usuário é a referência ativa.

Não há descarte silencioso de linhas. A interface pode solicitar remoção ou nova quantidade após receber o conflito.

## 5. Checkout e pedidos

Login é obrigatório para cotar entrega com intenção de compra e para criar pedido. O cliente envia apenas referências de endereço, apresentação, quantidades e a cotação selecionada; não envia preço confiável, subtotal, frete ou total como autoridade.

Na criação do pedido, o backend executa, na mesma unidade transacional quando possível:

1. carrega o carrinho e verifica titularidade;
2. revalida produto, apresentação, SKU, preço vigente e quantidade;
3. valida que o endereço pertence ao cliente e copia seus dados;
4. revalida a cotação de frete, sua validade, destino, área atendida, valor e prazo;
5. calcula subtotal, frete e total em decimal;
6. reserva o estoque com proteção contra concorrência;
7. cria o pedido, itens históricos e snapshot de entrega;
8. registra a chave de idempotência e limpa ou fecha o carrinho apenas depois de persistir o pedido.

Se preço, frete ou disponibilidade mudarem, a criação retorna `409 CHECKOUT_REVIEW_REQUIRED` com os valores atuais e a alteração identificada. Nenhum pedido parcial é confirmado. O cliente deve revisar e reenviar uma nova operação.

A criação exige `Idempotency-Key`. A chave é escopada ao usuário e ao endpoint. Mesma chave com o mesmo fingerprint de requisição devolve o resultado original, inclusive depois de uma falha de transporte; mesma chave com conteúdo diferente retorna `409 IDEMPOTENCY_KEY_REUSED`. A chave não é reutilizada para uma nova compra.

Pedidos são acessíveis por número público não sequencial ou UUID opaco, sempre com verificação do usuário autenticado. Listagem e detalhe de outro cliente respondem como recurso inexistente ou proibido conforme a política de exposição escolhida, sem revelar se o identificador existe.

## 6. Estoque

Cada apresentação possui:

- `physical_quantity`: unidades físicas registradas;
- `reserved_quantity`: unidades fisicamente presentes e comprometidas por reservas ativas;
- `available_quantity`: `physical_quantity - reserved_quantity`.

O estoque disponível nunca é armazenado como autoridade independente. A operação de reserva bloqueia a linha de estoque ou usa atualização condicional equivalente a `reserved_quantity + quantidade <= physical_quantity`. Duas transações concorrentes pela última unidade só permitem uma reserva.

Fluxo padrão:

- **Reservar**: na criação de pedido, se houver disponibilidade; cria uma reserva ativa com expiração configurável e incrementa reservado.
- **Converter**: após pagamento aprovado e validado, dentro de transação; decrementa reservado, decrementa físico, registra movimentação de venda e marca a reserva como convertida.
- **Liberar**: em cancelamento pré-pagamento, recusa, expiração ou falha definitiva; decrementa reservado e marca a reserva como liberada/expirada.
- **Expirar**: processo periódico identifica reservas ativas vencidas, com lock e referência idempotente, libera uma única vez e muda a reserva para `EXPIRED`.
- **Devolução física**: só incrementa físico depois de registro administrativo de recebimento e inspeção. Reembolso financeiro não altera estoque por si só.

Entrada e ajuste administrativo exigem quantidade, motivo, apresentação e responsável. Ajuste não pode gerar físico negativo nem reduzir físico abaixo do reservado. Uma movimentação usa chave de referência única ou operação idempotente para impedir baixa ou liberação duplicada.

## 7. Pagamentos

O checkout é hospedado pelo provedor. O backend cria a sessão de checkout com pedido, valor e moeda e armazena apenas identificadores, estados, valores, moeda e metadados necessários. Não armazena número de cartão, código de segurança, senha ou dados equivalentes.

O retorno do navegador serve apenas para conduzir o cliente de volta à loja. A loja consulta a API e mostra o estado já reconhecido pelo backend. O webhook valida assinatura, autenticidade, evento e janela temporal conforme o provedor; então confere pedido, tentativa, valor e `BRL` antes de aplicar qualquer efeito.

Eventos recebidos são registrados por identificador único e processados de forma idempotente. Eventos duplicados viram no-op depois do registro. Eventos fora de ordem são armazenados, mas só alteram o estado se forem compatíveis com a máquina de estados; a reconciliação consulta o provedor para resolver `UNKNOWN` e conflitos.

Timeout durante a criação ou consulta deixa a tentativa em `UNKNOWN` ou `PENDING`. Não se cria outra cobrança automaticamente sem consultar a tentativa existente. O cliente pode consultar o pedido e o backend pode reconciliar em tarefa controlada.

Se uma aprovação chegar após a reserva expirar:

1. a aprovação é registrada como válida e tardia;
2. o backend tenta, atomicamente, criar nova reserva e converter a venda se houver disponibilidade naquele instante;
3. se houver estoque, o pedido pode passar a `CONFIRMED` e a reserva é convertida;
4. se não houver estoque, o pedido fica `PAYMENT_REVIEW`, não é separado nem enviado, e é aberta pendência para reembolso ou solução administrativa;
5. nunca se confirma a venda apenas porque o pagamento foi aprovado.

O adaptador de pagamento simulado só existe em perfis de desenvolvimento e testes, com guarda explícita que impede inicialização em produção.

## 8. Frete, entrega e pós-venda

O módulo de frete expõe uma abstração de cotação com: destino normalizado, indicação de área atendida, modalidade, valor em BRL, prazo mínimo e máximo, versão da regra, expiração da cotação e identificador da cotação. Os valores e a área atendida permanecem pendentes de decisão comercial.

A recomendação técnica inicial é uma regra configurável no backend por faixa de CEP ou região operacional, sem hardcode no frontend. Enquanto a área, os valores e os prazos não forem aprovados, a implementação deve permitir configuração explícita de regiões atendidas e retornar destino não atendido; não deve inventar cobertura ou preço.

O pedido preserva o snapshot do endereço, modalidade, valor, prazo e versão da regra. Alterar o endereço do cliente não altera pedidos existentes.

Estados de entrega sugeridos: `NOT_STARTED`, `SEPARATING`, `SHIPPED`, `DELIVERED`, `RETURN_REQUESTED`, `RETURN_RECEIVED`, `CANCELED`. O envio registra transportadora opcional, código de rastreio opcional, data de envio e observação operacional. A entrega só pode ser registrada depois do envio.

Cancelamento é uma decisão do backend e depende dos estados:

- antes do pagamento: cancela pedido e libera reserva ativa, sem reembolso;
- pagamento aprovado antes do envio: requer ação administrativa, registra reembolso solicitado quando aplicável e não repõe estoque automaticamente; a reposição depende de confirmar que o produto continua fisicamente disponível;
- depois do envio ou entrega: não é cancelamento direto; registra solicitação de devolução e segue inspeção, reembolso e eventual retorno ao estoque em registros separados;
- reembolso solicitado não é reembolso confirmado. A confirmação vem do provedor ou de uma reconciliação manual documentada com evidência verificável, como referência da operação no painel, valor, moeda, data e operador; registrar uma intenção ou clicar em uma ação administrativa não confirma o reembolso por si só.

Uma reconciliação manual pode registrar a evidência recebida e, somente se ela indicar um resultado confirmado e for compatível com o pedido, mudar o registro para `CONFIRMED`. Evidência ausente, divergente ou apenas “solicitada” mantém `REQUESTED`/`PENDING` e abre pendência operacional.

## 9. Notificações

Pedido recebido, pagamento reconhecido, envio e recuperação de senha geram eventos transacionais. A entrega do e-mail é assíncrona ou reprocessável: a falha cria ou mantém uma notificação pendente com tentativas, próximo horário e erro sanitizado. Uma chave de evento impede envio duplicado causado por webhook repetido ou reprocessamento.

## 10. Relação com o frontend

O frontend pode formatar BRL e datas para apresentação, mas não pode somar, confirmar, reservar, aplicar descontos, estimar disponibilidade ou decidir ações comerciais. A API retorna valores monetários como strings decimais, disponibilidade e ações permitidas. Respostas privadas usam `Cache-Control: no-store` ou política equivalente; catálogos públicos só são cacheados segundo uma política que tolere a validade definida pelo backend.

## 11. Delimitação executável do estoque na Fase 5

A Fase 5 materializa saldo por apresentação com `physical_quantity`, `reserved_quantity` e disponibilidade calculada como `physical_quantity - reserved_quantity`. A migration inicializa nenhuma quantidade positiva: uma apresentação existente ou nova começa com saldo zero quando o saldo é materializado sob demanda.

As operações administrativas de entrada exigem quantidade positiva; `ADJUSTMENT` aceita um delta inteiro não nulo, inclusive negativo, desde que o físico não fique abaixo do reservado. Ambas exigem motivo, chave de idempotência e administrador. A baixa física de uma venda acontece na confirmação de uma reserva. Devoluções e reposição após inspeção permanecem operações específicas de fases posteriores.

Reservas persistidas usam os estados `ACTIVE`, `CONFIRMED` e `RELEASED`. Confirmação e liberação são transições terminais idempotentes para repetições do mesmo estado e incompatíveis entre si. Não há expiração automática na Fase 5; `expires_at` fica reservado para a integração do pedido na Fase 8.

O catálogo público expõe somente `availability.availableQuantity` e `availability.availableForPurchase`. Produto publicado com disponibilidade zero continua visível; falta de estoque não desativa, exclui ou despublica produto ou apresentação.

## 12. Endereços e frete na Fase 6

Endereços pertencem exclusivamente ao cliente autenticado pela sessão. O backend ignora qualquer tentativa de definir o proprietário no payload, normaliza o CEP para oito dígitos preservando zeros à esquerda, valida a UF brasileira e aplica limites aos demais campos. A consulta, alteração e remoção filtram simultaneamente por usuário e identificador; um identificador de outro cliente não revela nem altera o registro. A remoção é lógica para preservar referências futuras de pedidos.

A cotação de frete da Fase 6 exige sessão e CSRF, recebe somente o identificador de um endereço próprio e calcula valor e prazo no backend. A política inicial configurável usa faixas de CEP, uma modalidade, valor BRL decimal e prazo estimado em dias úteis. Destino fora das faixas retorna erro estruturado e não gera tarifa substituta. A cotação persistida guarda snapshot mínimo do destino, validade e versão da regra para futura revalidação.

Os valores e faixas locais/teste são sintéticos. A área comercial real, tarifa, modalidade e prazo ainda dependem do proprietário; o perfil de produção permanece sem oferta por padrão e falha de forma segura quando a configuração obrigatória não existe. Não há integração externa de CEP ou transportadora nesta fase.
