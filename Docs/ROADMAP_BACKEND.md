# PerfumeStore — Roadmap de Desenvolvimento do Backend

## 1. Objetivo

Construir do zero o backend de um e-commerce de perfumes, evoluindo da estrutura inicial até um MVP preparado para vendas reais.

O backend será responsável por todas as regras de negócio, validações, permissões, preços, estoque, frete, pedidos e pagamentos.

O frontend consumirá a API, enviará dados e apresentará os resultados, mensagens de erro e ações disponíveis.

---

## 2. Diretrizes do projeto

- Arquitetura de monólito modular, organizada por domínio.
- Java e Spring Boot, com versões compatíveis e suportadas verificadas na inicialização.
- PostgreSQL como banco de dados.
- Maven para dependências e build.
- Flyway para versionamento do banco.
- Spring Security para autenticação e autorização.
- API REST documentada com OpenAPI.
- Valores monetários tratados com precisão decimal.
- Configurações e credenciais fornecidas por ambiente.
- Desenvolvimento incremental, com testes dos comportamentos críticos.
- Integrações externas inicialmente em ambiente de testes.
- Cada fase deve deixar uma entrega executável e verificável.

### Limites do MVP

- Uma loja.
- Uma moeda: real brasileiro.
- Perfis CUSTOMER e ADMIN.
- Preço e estoque próprios para cada apresentação do perfume.
- Um provedor de pagamento.
- Uma modalidade inicial de cálculo de frete.
- Administração básica de produtos, estoque e pedidos.
- Cancelamentos e reembolsos tratados administrativamente.
- Sem cupons, marketplace, fidelidade, ERP ou recursos comerciais avançados.

---

## 3. Método de execução

Cada fase será dividida em tarefas menores quando necessário.

O fluxo será:

1. Definir o objetivo e os critérios de aceite.
2. Preparar um prompt de implementação para o Codex.
3. Implementar apenas o escopo daquela etapa.
4. Executar as verificações necessárias.
5. Apresentar alterações, evidências e pendências.
6. Revisar o resultado e corrigir problemas.
7. Registrar a entrega em commit.
8. Avançar para a próxima etapa.

Uma fase não será considerada concluída apenas porque o código foi escrito.

---

## Fase 0 — Especificação e decisões fundamentais

### Objetivo

Transformar o escopo do MVP em regras claras antes de iniciar a implementação.

### Atividades

- Registrar funcionalidades incluídas e excluídas.
- Definir os módulos do backend.
- Modelar as principais entidades e relacionamentos.
- Definir os contratos iniciais da API.
- Definir o padrão de erros gerais e por campo.
- Definir a estratégia de autenticação, sessão e logout.
- Definir como o carrinho de visitante será identificado e associado à conta.
- Definir a regra de frete e a área de atendimento.
- Definir os estados de pedido, pagamento e entrega.
- Definir quando o estoque será reservado, confirmado e liberado.
- Definir o tratamento de pagamentos aprovados após a expiração da reserva.
- Registrar as integrações necessárias: pagamento, e-mail e imagens.

### Modelo inicial de domínios

- Autenticação e usuários.
- Endereços.
- Catálogo e categorias.
- Estoque.
- Carrinho.
- Entrega e frete.
- Pedidos.
- Pagamentos.
- Notificações.
- Administração.

### Critério de conclusão

Escopo, regras centrais e decisões de arquitetura documentados, com pendências explicitamente identificadas.

---

## Fase 1 — Inicialização e infraestrutura local

### Objetivo

Criar uma base reproduzível para desenvolver e executar o backend.

### Atividades

- Criar um novo projeto e repositório, preservando o projeto anterior.
- Configurar Java, Spring Boot e Maven Wrapper.
- Organizar os pacotes por domínio.
- Configurar PostgreSQL local com Docker Compose.
- Configurar Flyway e a primeira migration.
- Separar configurações de desenvolvimento, testes e produção.
- Criar exemplos de configuração sem credenciais reais.
- Configurar logs básicos e endpoint de saúde.
- Criar README com instruções de execução.
- Configurar integração contínua para build e testes.

### Critério de conclusão

Uma instalação limpa consegue iniciar o banco e a aplicação seguindo o README, com migrations executadas e build aprovado.

---

## Fase 2 — Padrões da API e base de segurança

### Objetivo

Estabelecer os padrões usados por todos os módulos.

### Atividades

- Configurar DTOs de entrada e saída.
- Configurar validação dos dados no servidor.
- Implementar tratamento global de exceções.
- Padronizar erros gerais e erros associados a campos.
- Definir paginação, ordenação e limites de consulta.
- Definir tratamento de datas, horários e valores monetários.
- Configurar OpenAPI.
- Configurar Spring Security com acesso restrito por padrão.
- Configurar CORS e proteção contra CSRF conforme a estratégia de autenticação.
- Garantir que erros e logs não exponham segredos ou detalhes internos.

### Critério de conclusão

A API responde de forma consistente a entradas inválidas e acessos não autorizados, com documentação inicial disponível.

---

## Fase 3 — Autenticação, usuários e recuperação de acesso

### Objetivo

Permitir acesso seguro de clientes e administradores.

### Atividades

- Implementar cadastro de cliente.
- Implementar login, consulta do usuário autenticado e logout.
- Implementar expiração, renovação e revogação conforme a estratégia de sessão.
- Armazenar senhas com hash apropriado.
- Implementar perfis CUSTOMER e ADMIN.
- Impedir que o cadastro público conceda privilégios administrativos.
- Definir criação segura do primeiro administrador.
- Implementar consulta e atualização dos dados permitidos.
- Implementar recuperação de senha com token temporário e de uso único.
- Integrar envio de e-mail em ambiente de desenvolvimento.
- Aplicar proteção contra tentativas excessivas em endpoints sensíveis.

### Verificações essenciais

- Cadastro duplicado e dados inválidos.
- Login correto e incorreto.
- Sessão expirada e logout.
- Cliente tentando acessar funções administrativas.
- Recuperação com token inválido, expirado ou reutilizado.
- Ausência de exposição de senha e tokens nas respostas e logs.

### Critério de conclusão

Cliente e administrador conseguem autenticar com permissões distintas, e a recuperação de acesso funciona em ambiente de testes.

---

## Fase 4 — Catálogo, categorias e imagens

### Objetivo

Disponibilizar os produtos para consulta e administração.

### Atividades

- Implementar categorias.
- Implementar produtos com nome, marca e descrição.
- Implementar apresentações com identificador próprio, volume e preço.
- Permitir ativação e desativação de produtos e apresentações.
- Implementar listagem pública paginada.
- Implementar busca por nome e filtro por categoria.
- Implementar consulta dos detalhes de um produto.
- Implementar endpoints administrativos de cadastro e edição.
- Integrar armazenamento persistente de imagens.
- Validar tamanho e tipo dos arquivos no backend.
- Permitir ordenação das imagens e definição da imagem principal.
- Preservar referências de produtos já utilizados em pedidos.

### Critério de conclusão

O administrador consegue manter o catálogo, e a API pública apresenta apenas os produtos disponíveis para publicação.

---

## Fase 5 — Estoque e disponibilidade

### Objetivo

Controlar estoque por apresentação e preparar a proteção contra vendas acima da disponibilidade.

### Atividades

- Registrar quantidade física, reservada e disponível.
- Implementar entrada e ajuste de estoque com motivo.
- Registrar movimentações e responsável pelo ajuste.
- Impedir estoque negativo.
- Implementar operações transacionais de reserva, confirmação e liberação.
- Garantir que reservas concorrentes respeitem a disponibilidade.
- Expor disponibilidade no catálogo sem divulgar dados administrativos.

### Verificações essenciais

- Ajustes inválidos.
- Reserva acima da disponibilidade.
- Duas tentativas concorrentes de reservar a última unidade.
- Confirmação ou liberação repetida de uma mesma reserva.

### Critério de conclusão

As operações de estoque mantêm consistência, inclusive com requisições simultâneas.

---

## Fase 6 — Endereços e frete

### Objetivo

Definir onde a loja entrega e quanto será cobrado.

### Atividades

- Implementar cadastro, consulta, edição e remoção de endereços.
- Garantir que cada cliente acesse somente seus endereços.
- Validar os campos no servidor.
- Implementar a regra inicial de frete definida na Fase 0.
- Validar a área de atendimento.
- Retornar valor e prazo estimado da entrega.
- Rejeitar destinos não atendidos com mensagem clara.
- Manter regras e valores de frete configuráveis no backend.

### Critério de conclusão

A API consegue validar o destino e calcular uma entrega real dentro da área atendida.

---

## Fase 7 — Carrinho persistente

### Objetivo

Permitir a montagem da compra com dados e cálculos controlados pelo servidor.

### Atividades

- Criar carrinho para visitante com identificação segura.
- Criar ou recuperar o carrinho do cliente.
- Implementar associação e combinação dos carrinhos no login.
- Adicionar e remover apresentações.
- Alterar quantidades.
- Consultar o carrinho com itens e subtotal calculados.
- Validar produtos inativos e quantidades indisponíveis.
- Atualizar os valores conforme os preços vigentes.
- Retornar mensagens sobre itens que precisam de ajuste.
- Definir expiração e limpeza de carrinhos abandonados.

### Regra importante

Adicionar ao carrinho não reserva estoque. A reserva ocorrerá na finalização da compra.

### Critério de conclusão

Visitantes e clientes conseguem manter carrinhos, e o servidor devolve valores e disponibilidade atualizados após cada operação.

---

## Fase 8 — Checkout e criação de pedidos

### Objetivo

Transformar o carrinho em um pedido consistente.

### Atividades

- Exigir autenticação para finalizar a compra.
- Validar carrinho, endereço e entrega.
- Revalidar preços e disponibilidade.
- Calcular subtotal, frete e total.
- Registrar uma cópia dos dados comerciais dos itens e do endereço no pedido.
- Criar número de pedido.
- Reservar estoque de forma transacional.
- Implementar idempotência para evitar pedidos duplicados em tentativas repetidas.
- Separar status do pedido, do pagamento e da entrega.
- Implementar consulta e histórico de pedidos do cliente.
- Implementar expiração das reservas de pedidos não pagos.
- Criar um adaptador de pagamento simulado, restrito a desenvolvimento e testes.

### Verificações essenciais

- Preço ou total adulterado pelo cliente.
- Produto desativado após ser adicionado ao carrinho.
- Estoque esgotado durante a finalização.
- Reenvio da mesma solicitação.
- Acesso ao pedido de outro cliente.
- Alteração de produto ou endereço sem modificar o histórico do pedido.

### Critério de conclusão

É possível realizar uma compra simulada completa pela API, com pedido registrado, estoque consistente e acesso protegido.

### Marco de entrega

Backend comercial básico disponível para iniciar a integração do frontend.

---

## Fase 9 — Integração com o provedor de pagamento

### Objetivo

Conectar os pedidos a um meio real de pagamento.

### Atividades

- Integrar um único provedor em sandbox.
- Criar checkout hospedado a partir do pedido.
- Registrar identificadores e tentativas de pagamento.
- Tratar falhas e respostas incertas sem gerar cobranças duplicadas.
- Implementar recebimento e validação de webhooks.
- Conferir pedido, valor e moeda antes de reconhecer o pagamento.
- Processar notificações repetidas e fora de ordem.
- Confirmar a movimentação de estoque após aprovação válida.
- Tratar pagamento recusado, cancelado ou expirado.
- Aplicar a regra definida para aprovação após expiração da reserva.
- Consultar o provedor para reconciliar pagamentos pendentes.
- Impedir que o retorno do navegador confirme o pagamento.
- Garantir que o pagamento simulado não possa ser usado em produção.

### Critério de conclusão

O ciclo de pagamento é homologado em sandbox, incluindo aprovação, recusa, expiração, repetição de eventos e recuperação de falhas.

---

## Fase 10 — Operação administrativa e pós-venda

### Objetivo

Permitir que a loja processe as vendas recebidas.

### Atividades

- Listar pedidos com paginação e filtros básicos.
- Consultar itens, cliente, pagamento e endereço do pedido.
- Registrar início da separação.
- Registrar envio, transportadora e rastreamento quando aplicável.
- Registrar conclusão da entrega.
- Validar transições de status no backend.
- Implementar cancelamento conforme situação do pedido e pagamento.
- Registrar solicitações e andamento de reembolsos.
- Usar o provedor como referência para confirmar reembolso.
- Permitir operação manual no painel do provedor quando essa for a opção do MVP.
- Registrar devoluções e reposição de estoque separadamente do reembolso.
- Manter histórico das ações administrativas relevantes.

### Critério de conclusão

O administrador consegue acompanhar uma venda até a entrega e tratar cancelamentos ou devoluções sem perder a consistência dos registros.

---

## Fase 11 — Notificações transacionais

### Objetivo

Comunicar os eventos essenciais ao cliente.

### Atividades

- Finalizar integração com o serviço de e-mail.
- Criar mensagens de pedido recebido, pagamento aprovado e pedido enviado.
- Revisar as mensagens de recuperação de senha.
- Garantir que falha de e-mail não desfaça um pedido ou pagamento.
- Registrar envios pendentes e implementar novas tentativas.
- Evitar disparos repetidos causados pelo reprocessamento do mesmo evento.
- Manter links e informações adequados a cada ambiente.

### Critério de conclusão

As mensagens essenciais são enviadas e falhas temporárias podem ser recuperadas sem afetar a venda.

---

## Fase 12 — Homologação integrada e preparação operacional

### Objetivo

Verificar o backend completo e preparar sua execução fora do ambiente local.

### Atividades

- Executar testes de integração com PostgreSQL.
- Validar jornadas completas de cliente e administrador.
- Revisar permissões e acesso a recursos de outros usuários.
- Validar concorrência de estoque e idempotência.
- Validar recuperação após falhas de pagamento e notificações.
- Revisar logs para evitar exposição de dados sensíveis.
- Finalizar documentação OpenAPI e exemplos de integração.
- Configurar imagem Docker de produção.
- Configurar ambiente de homologação com HTTPS.
- Configurar limites de requisições e uploads.
- Configurar monitoramento básico e identificação de falhas.
- Configurar backup e verificar restauração.
- Documentar execução de migrations e procedimento de recuperação de deploy.

### Critério de conclusão

O backend funciona em homologação, passa pelos cenários críticos e possui procedimentos básicos de operação e recuperação.

---

## Fase 13 — Integração final e liberação do MVP

### Objetivo

Concluir a entrega do backend com evidências de funcionamento no fluxo real da loja.

### Atividades

- Apoiar a integração com o frontend.
- Corrigir diferenças entre contrato documentado e comportamento da API.
- Validar erros por campo e mensagens exibidas ao cliente.
- Configurar credenciais e serviços de produção.
- Cadastrar o administrador e o catálogo inicial.
- Confirmar preços, estoque e regras de entrega.
- Garantir separação entre dados de teste e dados reais.
- Validar domínio, HTTPS, e-mails, imagens e webhooks.
- Realizar compra controlada de ponta a ponta.
- Conferir pedido, pagamento, estoque, comunicação e operação de envio.
- Validar o procedimento de cancelamento e reembolso.
- Registrar versão e evidências de homologação.

### Critério de conclusão

O cliente consegue comprar pelo frontend integrado, e o administrador consegue processar a venda com pagamento, estoque e entrega consistentes.

### Dependência

A conclusão desta fase depende do frontend e da configuração dos serviços externos. Até lá, o backend poderá estar homologado pela API, mas o MVP completo ainda não estará finalizado.

---

## 4. Marcos do roadmap

| Marco | Fases concluídas | Resultado |
|---|---|---|
| Base técnica | 0 a 2 | Projeto executável, padrões de API e segurança inicial |
| Identidade e catálogo | 3 e 4 | Usuários, acesso, produtos e imagens |
| Compra simulada | 5 a 8 | Estoque, frete, carrinho e pedidos funcionando |
| Operação comercial | 9 a 11 | Pagamento integrado, administração e notificações |
| Backend homologado | 12 | API validada e ambiente operacional preparado |
| MVP completo | 13 | Backend e frontend integrados para vendas reais |

---

## 5. Definição de conclusão do backend

- [ ] Cadastro, login, logout e recuperação de senha funcionando.
- [ ] Permissões de cliente e administrador verificadas.
- [ ] Catálogo e imagens administráveis.
- [ ] Preço e estoque separados por apresentação.
- [ ] Carrinho de visitante e cliente funcionando.
- [ ] Endereços e frete validados no servidor.
- [ ] Checkout com cálculo integral no backend.
- [ ] Pedidos protegidos contra duplicação.
- [ ] Estoque consistente em compras simultâneas.
- [ ] Pagamento e webhooks homologados.
- [ ] Expiração de reservas e pagamentos tardios tratados.
- [ ] Administração de pedidos e envio funcionando.
- [ ] Cancelamento, reembolso e devolução com procedimentos definidos.
- [ ] E-mails essenciais funcionando.
- [ ] API documentada.
- [ ] Testes dos fluxos críticos aprovados.
- [ ] Logs, monitoramento e backup configurados.
- [ ] Restauração de backup verificada.
- [ ] Compra completa validada com o frontend.
- [ ] Ambiente de produção preparado e homologado.

---

## 6. Próxima etapa

Iniciar pela Fase 0, registrando as regras de negócio e as decisões que orientarão a implementação.

O roadmap do frontend será elaborado separadamente, usando os contratos e os marcos deste backend como referência.

