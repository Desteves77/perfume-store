# PerfumeStore — Roadmap de Desenvolvimento do Frontend

## 1. Objetivo

Construir uma loja virtual de perfumes com apresentação profissional, navegação confortável no celular e integração completa com o backend do MVP.

O frontend incluirá a loja pública, a área do cliente e o painel administrativo.

Este documento complementa o ROADMAP_BACKEND.md. As fases de frontend e backend têm numerações independentes; as dependências estão indicadas neste roadmap.

**Status inicial:** planejamento. Nenhuma fase descrita aqui deve ser interpretada como implementada ou homologada.

---

## 2. Divisão de responsabilidades

### Backend

- Validar todos os dados enviados.
- Autenticar usuários e autorizar operações.
- Determinar preços, disponibilidade, quantidades permitidas, frete e totais.
- Administrar carrinho, estoque, pedidos, pagamentos e devoluções.
- Determinar transições de status e ações comerciais permitidas.
- Fornecer erros gerais e erros identificados por campo.
- Confirmar pagamento por integração validada com o provedor.

### Frontend

- Organizar telas, navegação e componentes.
- Coletar dados e enviá-los à API.
- Exibir os dados e valores retornados pelo backend.
- Apresentar mensagens de validação retornadas pelo servidor.
- Controlar estado visual de campos, menus, modais e carregamento.
- Formatar moeda e datas para apresentação, sem recalcular valores comerciais.
- Atualizar a interface após respostas da API.

### Regras obrigatórias

- Não implementar validações próprias de negócio no frontend.
- Não duplicar schemas de validação de entrada do backend no cliente.
- Não usar a validação automática do navegador para substituir o fluxo de erros do servidor; os formulários devem permitir que a API receba e valide os dados.
- Preservar rótulos, indicações acessíveis e teclados adequados ao preenchimento.
- Não calcular subtotal, frete, descontos ou total no navegador.
- Não manter estoque, carrinho ou status de pagamento como verdade apenas local.
- Não reproduzir regras comerciais em rotas de servidor do Next.js. O backend Java continua sendo a autoridade.
- Componentes podem ocultar opções conforme permissões e ações informadas pela API; a autorização efetiva permanece no backend.
- Durante mutações, mostrar carregamento e aguardar a confirmação da API antes de apresentar o resultado comercial como concluído.
- Desabilitar um botão durante uma requisição é uma proteção de interface, não uma garantia contra duplicação no servidor.

---

## 3. Base técnica proposta

- Next.js com TypeScript.
- Organização por funcionalidades e componentes reutilizáveis.
- Tailwind para estilos, com tokens visuais centralizados.
- Camada central de acesso à API.
- Contratos de resposta alinhados à documentação OpenAPI.
- Estratégia de sessão definida em conjunto com o backend.
- Configuração da URL da API por ambiente.
- Versões e compatibilidade das dependências verificadas na inicialização.
- Um projeto de frontend com áreas pública, autenticada e administrativa.

### Limites do MVP

- Uma loja, em português e com valores em reais.
- Catálogo, busca e filtro simples por categoria.
- Conta, endereços, carrinho, checkout e pedidos.
- Checkout de pagamento hospedado por um provedor.
- Painel administrativo básico.
- Páginas institucionais e canal de atendimento.
- Sem favoritos, avaliações, cupons, fidelidade, editor visual de páginas ou relatórios avançados.
- Sem formulário próprio para capturar dados de cartão.

---

## 4. Direção visual

A proposta inicial é uma loja elegante, legível e centrada nas fotografias dos perfumes.

### Direção sugerida, a validar antes da implementação visual

- Base clara, com tons neutros e uma cor de destaque.
- Tipografia de títulos com personalidade e textos de leitura fácil.
- Fotos padronizadas, com proporções consistentes.
- Espaçamento confortável e hierarquia clara de informações.
- Botões e links com comportamento previsível.
- Animações discretas, sem atrasar a navegação.
- Identidade própria para a perfumaria.
- Layout pensado primeiro para celular e ampliado para computador.

### Elementos obrigatórios

- Contraste adequado.
- Navegação por teclado e foco visível.
- Campos com rótulos e associação às mensagens de erro.
- Textos alternativos nas imagens relevantes.
- Estados de carregamento, erro, vazio e sucesso.
- Ausência de rolagem horizontal indevida.
- Respeito à preferência de redução de movimento.

A paleta, a tipografia, o nome comercial e os materiais de marca ainda precisam ser definidos. Não usar selos, avaliações, descontos ou promessas comerciais fictícias.

---

## 5. Método de execução

1. Delimitar a fase e verificar as dependências da API.
2. Preparar um prompt específico para o Codex.
3. Implementar os componentes e telas previstos.
4. Integrar os endpoints disponíveis.
5. Verificar comportamento e aparência.
6. Corrigir os problemas encontrados.
7. Registrar evidências e commit.
8. Atualizar o andamento sem marcar pendências como concluídas.

Referências estáticas podem ser usadas na definição visual. Dados de exemplo não substituem a integração nos critérios de aceite nem podem aparecer como dados reais na versão publicada.

---

## Fase 0 — Jornadas, contratos e identidade visual

### Objetivo

Definir o que será exibido e como o usuário percorrerá a loja.

### Atividades

- Ler o ROADMAP_BACKEND.md.
- Mapear jornadas do visitante, cliente e administrador.
- Definir navegação e inventário de páginas.
- Revisar os contratos iniciais da API e o padrão de erros.
- Registrar os dados e ações necessários em cada tela.
- Definir como preservar a intenção de compra durante o login.
- Definir nome, logo, paleta, tipografia e padrão de fotografias.
- Criar referências visuais para home, produto, carrinho e checkout.
- Definir também o padrão visual do painel administrativo.
- Registrar necessidades de API ainda não atendidas, sem implementá-las no cliente.

### Critério de conclusão

Mapa de telas, direção visual e contratos necessários registrados e revisados.

### Dependência do backend

Fase 0: especificação e decisões fundamentais.

---

## Fase 1 — Inicialização e estrutura do frontend

### Objetivo

Criar uma base organizada e reproduzível.

### Atividades

- Inicializar Next.js e TypeScript.
- Configurar estilos e tokens visuais.
- Organizar funcionalidades, componentes e acesso à API.
- Separar layouts da loja, conta e administração.
- Configurar ambientes e exemplos sem segredos.
- Configurar lint, verificação de tipos e build.
- Criar README de execução.
- Configurar integração contínua.
- Definir comportamento de páginas inexistentes e falhas inesperadas.
- Garantir que segredos não sejam incluídos no código entregue ao navegador.

### Critério de conclusão

O projeto executa localmente e passa pelas verificações básicas em uma instalação limpa.

---

## Fase 2 — Componentes visuais e estrutura de navegação

### Objetivo

Estabelecer uma interface consistente antes de montar as jornadas completas.

### Atividades

- Criar botões, links, campos, seletores, alertas e indicadores de status.
- Criar modal de confirmação, notificações e estados de carregamento.
- Criar cards de produtos e componentes de imagem.
- Criar cabeçalho com logo, catálogo, busca, conta e carrinho.
- Criar navegação móvel acessível.
- Criar rodapé com contato e páginas institucionais.
- Criar estrutura da área do cliente.
- Criar estrutura do painel administrativo.
- Verificar foco, contraste e comportamento em diferentes larguras.

### Critério de conclusão

Os componentes e layouts fundamentais estão utilizáveis em celular e computador, com estados visuais definidos.

---

## Fase 3 — Integração com a API e sessão

### Objetivo

Centralizar a comunicação com o backend.

### Atividades

- Implementar cliente HTTP e configuração por ambiente.
- Tipar requisições e respostas conforme os contratos.
- Centralizar apresentação de erros gerais e por campo.
- Implementar comportamento para acesso não autenticado, proibido e sessão expirada.
- Implementar o ciclo de sessão conforme a estratégia do backend.
- Definir envio de credenciais e proteção CSRF quando aplicável.
- Evitar armazenamento improvisado de credenciais no navegador.
- Separar cache de catálogo público dos dados privados do usuário.
- Limpar dados privados em logout e troca de conta.
- Tratar falha de conexão e permitir nova tentativa.
- Evitar repetição automática de operações comerciais sem garantia de idempotência.
- Consultar ações permitidas retornadas pelo servidor quando necessário.

### Critério de conclusão

A comunicação com a API real funciona, incluindo erros e expiração de sessão, sem duplicar regras comerciais.

### Dependência do backend

Fases 2 e 3: padrões da API, segurança e autenticação.

---

## Fase 4 — Home, catálogo e página de produto

### Objetivo

Permitir que o cliente descubra e escolha perfumes.

### Atividades

- Construir a home com banner, categorias e seleção de produtos.
- Usar uma regra simples de seleção de produtos acordada com o backend.
- Implementar catálogo com paginação, busca e filtro por categoria.
- Preservar busca e filtros na URL.
- Criar página de produto com fotos, marca e descrição.
- Exibir apresentações, volumes e preços fornecidos pela API.
- Exibir disponibilidade e indicação de produto esgotado.
- Preparar a ação de adicionar ao carrinho para integração na Fase 6.
- Tratar produto inexistente ou desativado.
- Tratar busca vazia e falha de carregamento.
- Otimizar dimensões e carregamento das imagens.

### Critério de conclusão

Home, catálogo e detalhes exibem produtos reais da API, com boa apresentação em celular e computador.

### Dependência do backend

Fases 4 e 5: catálogo, imagens e disponibilidade.

---

## Fase 5 — Conta, autenticação e endereços

### Objetivo

Permitir acesso e manutenção dos dados do cliente.

### Atividades

- Implementar cadastro, login e logout.
- Implementar solicitação e conclusão da recuperação de senha.
- Criar área Minha conta.
- Exibir e editar os dados permitidos pelo backend.
- Implementar listagem, cadastro, edição e remoção de endereços.
- Apresentar todos os erros de validação fornecidos pela API.
- Preservar o destino da navegação após login, usando destinos internos seguros.
- Tratar links de recuperação inválidos, expirados e já utilizados.
- Tratar sessão expirada durante uma ação.

### Critério de conclusão

O cliente consegue acessar sua conta, recuperar acesso e manter endereços usando a API real.

### Dependência do backend

Fases 3 e 6: usuários e endereços.

---

## Fase 6 — Carrinho

### Objetivo

Permitir montar e revisar a compra com o carrinho mantido pelo servidor.

### Atividades

- Integrar adição ao carrinho a partir da página de produto.
- Exibir itens, fotos, apresentações e valores retornados pela API.
- Implementar alteração de quantidade e remoção.
- Atualizar o indicador do carrinho após confirmação do servidor.
- Exibir subtotal informado pelo backend.
- Suportar carrinho de visitante e de cliente.
- Apresentar o resultado da associação de carrinhos após login.
- Informar indisponibilidade ou mudança de preço conforme resposta da API.
- Criar estado de carrinho vazio.
- Preservar o estado coerente em falhas de requisição.

### Critério de conclusão

O carrinho funciona antes e depois do login e permanece consistente após recarregar a página.

### Dependência do backend

Fase 7: carrinho persistente.

---

## Fase 7 — Checkout e criação do pedido

### Objetivo

Conduzir o cliente à finalização sem ambiguidade sobre itens e valores.

### Atividades

- Exigir login para acessar a finalização.
- Permitir selecionar ou cadastrar endereço.
- Solicitar cálculo de entrega ao backend.
- Exibir frete, prazo estimado e eventual indisponibilidade do destino.
- Exibir resumo dos itens, subtotal, frete e total retornados pela API.
- Invalidar visualmente uma cotação antiga ao mudar endereço ou itens e solicitar uma nova.
- Enviar a solicitação de criação do pedido.
- Integrar a chave de idempotência conforme o contrato da API.
- Preservar essa chave nas tentativas da mesma operação.
- Consultar o resultado antes de repetir uma criação cujo retorno ficou incerto.
- Apresentar alterações comerciais retornadas pelo backend para revisão.
- Tratar estoque esgotado e falhas de conexão.

### Critério de conclusão

A compra simulada cria um pedido real no ambiente de testes, com valores calculados pelo backend e sem duplicação em reenvios.

### Dependência do backend

Fase 8: checkout e pedidos.

### Marco de entrega

Loja demonstrável de ponta a ponta com pagamento simulado, restrito ao ambiente de testes.

---

## Fase 8 — Pagamento e acompanhamento de pedidos

### Objetivo

Integrar o checkout hospedado e tornar o andamento da compra compreensível.

### Atividades

- Encaminhar o cliente ao checkout fornecido pelo backend.
- Criar página de retorno do pagamento.
- Consultar a API para determinar a situação real do pagamento.
- Exibir estados pendente, aprovado, recusado ou expirado.
- Não confiar em parâmetros da URL como prova de pagamento.
- Permitir atualização do status com consultas limitadas e controladas.
- Exibir novas tentativas somente quando permitidas pelo backend.
- Criar histórico paginado de pedidos.
- Criar detalhes com itens, valores, endereço, pagamento e entrega.
- Exibir rastreamento quando disponível.
- Apresentar canal de atendimento para cancelamento e devolução.
- Tratar acessos sem permissão sem revelar dados do pedido.

### Critério de conclusão

O cliente consegue pagar em sandbox, retornar à loja e acompanhar os dados verdadeiros do pedido.

### Dependência do backend

Fases 9 e 10: pagamento e operação dos pedidos.

---

## Fase 9 — Administração do catálogo e estoque

### Objetivo

Disponibilizar as telas necessárias para manter os produtos da loja.

### Atividades

- Integrar o acesso administrativo ao perfil retornado pela API.
- Criar listagem de produtos com busca e paginação.
- Criar formulários de categorias e produtos.
- Permitir ativação e desativação.
- Implementar apresentações com volume e preço.
- Implementar upload, ordenação e remoção de imagens.
- Exibir estoque físico, reservado e disponível conforme a API.
- Criar formulário de ajuste de estoque com motivo.
- Exibir movimentações básicas.
- Exigir confirmação visual para ações de maior impacto.
- Tratar erros de upload e conflitos retornados pelo servidor.

### Critério de conclusão

O administrador consegue manter catálogo, fotos e estoque sem usar diretamente ferramentas de API.

### Dependência do backend

Fases 4 e 5: administração do catálogo e estoque.

---

## Fase 10 — Administração dos pedidos e pós-venda

### Objetivo

Permitir a operação diária das vendas.

### Atividades

- Criar listagem paginada de pedidos com filtros básicos.
- Exibir detalhes do cliente, itens, valores, pagamento e entrega.
- Apresentar ações permitidas retornadas pelo backend.
- Registrar separação, envio e conclusão.
- Permitir informar transportadora e rastreamento.
- Exibir histórico do pedido.
- Disponibilizar cancelamento conforme ações autorizadas pela API.
- Registrar e acompanhar solicitações de reembolso.
- Distinguir solicitação de reembolso de reembolso efetivamente confirmado.
- Disponibilizar registro de devolução e ajuste de estoque conforme o fluxo do backend.
- Tratar alterações concorrentes, atualizando os dados após conflito.
- Evitar botão genérico que marque um pedido arbitrariamente como pago.

### Critério de conclusão

O administrador consegue operar uma venda até a entrega e acompanhar o pós-venda pelo painel.

### Dependência do backend

Fase 10: operação administrativa e pós-venda.

---

## Fase 11 — Conteúdo, qualidade visual e preparação para publicação

### Objetivo

Finalizar a experiência da loja e seu conteúdo público.

### Atividades

- Revisar todas as páginas em celular e computador.
- Padronizar títulos, espaçamentos, botões, formulários e mensagens.
- Completar estados de carregamento, vazio, erro e sucesso.
- Revisar navegação por teclado, foco e leitura das mensagens.
- Incluir conteúdo aprovado de contato, entrega, trocas, privacidade e termos.
- Inserir dados reais da loja e seus canais de atendimento.
- Configurar títulos, descrições, favicon e imagens de compartilhamento.
- Configurar sitemap e indexação das páginas públicas relevantes.
- Impedir indexação de conta, checkout e administração, sem tratar isso como controle de acesso.
- Configurar ambiente de homologação para não ser indexado.
- Revisar desempenho, imagens e estabilidade visual.
- Revisar cache para evitar exibição de dados privados entre contas ou valores comerciais desatualizados.
- Remover dados fictícios e elementos provisórios da versão destinada à produção.

### Critério de conclusão

A loja tem identidade consistente, conteúdo real e experiência adequada nos tamanhos de tela acordados.

---

## Fase 12 — Homologação integrada e lançamento do MVP

### Objetivo

Validar o sistema completo e publicar a versão preparada para operação.

### Atividades

- Publicar o frontend em homologação e conectar ao backend correspondente.
- Validar cadastro, login, recuperação de senha e endereços.
- Validar carrinho de visitante e associação após login.
- Validar catálogo, busca, seleção de apresentação e checkout.
- Homologar pagamento aprovado, pendente, recusado e expirado.
- Testar recarregamento, navegação de retorno e perda de conexão durante a compra.
- Verificar ausência de duplicação de pedidos.
- Validar dados privados, logout e troca de conta.
- Validar cadastro de produtos e operação administrativa.
- Conferir e-mails e links enviados pelo backend.
- Executar testes automatizados dos fluxos críticos e revisão visual manual.
- Corrigir bloqueios encontrados na homologação.
- Configurar domínio, HTTPS, URL da API e sessão de produção.
- Validar CORS, cookies e CSRF na topologia real do deploy.
- Realizar compra controlada e conferir pedido, pagamento, estoque e comunicação.
- Validar operação de envio, cancelamento e reembolso.
- Registrar a versão publicada, evidências e procedimento de retorno à versão anterior.

### Critério de conclusão

Cliente e administrador conseguem completar suas jornadas na versão publicada, com backend integrado e sem falhas críticas conhecidas.

### Dependência do backend

Fases 11 a 13: notificações, homologação e liberação do MVP.

---

## 6. Sequência conjunta com o backend

| Momento | Backend | Frontend |
|---|---|---|
| Definição | Fase 0 | Fase 0 pode ser preparada |
| Backend básico | Fases 1 a 8 | Identidade e componentes podem ser planejados; integração comercial aguarda os endpoints |
| Loja apresentável | Compra simulada disponível | Fases 1 a 7 |
| Operação completa | Fases 9 a 11 | Fases 8 a 10 |
| Acabamento | Fase 12 | Fase 11 |
| MVP final | Fase 13 | Fase 12 |

A prioridade acordada é construir primeiro o backend comercial básico, depois o frontend apresentável e, em seguida, concluir integrações, acabamento e homologação.

---

## 7. Inventário mínimo de páginas

### Loja pública

- Home.
- Catálogo com busca e filtro.
- Detalhes do produto.
- Carrinho.
- Login e cadastro.
- Solicitação de recuperação e redefinição de senha.
- Contato e informações de entrega.
- Trocas e devoluções.
- Privacidade e termos.
- Página não encontrada e tratamento de falha inesperada.

### Área do cliente

- Minha conta.
- Endereços.
- Checkout.
- Retorno e situação do pagamento.
- Meus pedidos.
- Detalhes do pedido.

### Administração

- Produtos e formulário de produto.
- Categorias.
- Apresentações e imagens, integradas à edição do produto.
- Estoque e movimentações.
- Pedidos e detalhes.
- Ações de envio, cancelamento e pós-venda integradas ao pedido.

Não é necessário construir um dashboard com gráficos para o MVP. A entrada administrativa pode abrir diretamente a lista de pedidos.

---

## 8. Definição de conclusão do frontend

- [ ] Interface responsiva e identidade visual consistente.
- [ ] Catálogo e produtos consumindo dados reais.
- [ ] Cadastro, login, logout e recuperação de acesso funcionando.
- [ ] Dados do cliente e endereços integrados.
- [ ] Carrinho persistente de visitante e cliente funcionando.
- [ ] Checkout exibindo os valores retornados pelo backend.
- [ ] Pagamento hospedado integrado.
- [ ] Situação do pagamento obtida exclusivamente pela API.
- [ ] Histórico, detalhes e rastreamento dos pedidos funcionando.
- [ ] Administração de produtos, imagens e estoque funcionando.
- [ ] Administração de pedidos e pós-venda funcionando.
- [ ] Validações e regras comerciais concentradas no backend.
- [ ] Estados de erro, vazio, carregamento e sucesso completos.
- [ ] Navegação por teclado e acessibilidade básica verificadas.
- [ ] Conteúdo institucional real inserido.
- [ ] Imagens e desempenho revisados.
- [ ] Nenhum segredo exposto no bundle do navegador.
- [ ] Nenhum dado privado compartilhado entre sessões.
- [ ] Fluxos críticos testados.
- [ ] Compra completa homologada com o backend.
- [ ] Deploy, domínio e configuração de produção verificados.

---

## 9. Orientação de uso pelo Codex

- Consultar este arquivo junto com ROADMAP_BACKEND.md.
- Verificar o estado real do repositório antes de iniciar uma fase.
- Respeitar o escopo do prompt da etapa em execução.
- Não presumir que uma fase anterior foi concluída sem evidências.
- Registrar incompatibilidades com a API e corrigir o contrato no módulo responsável.
- Não contornar ausência de endpoints com regras comerciais no frontend.
- Não apresentar dados simulados como integração concluída.
- Documentar decisões e pendências encontradas.
- Ao concluir uma etapa, informar alterações, verificações realizadas e limitações restantes.

