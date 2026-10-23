# Guia de substituição do provedor de pagamento
## Escopo
A Fase 9 implementa somente a infraestrutura e o adaptador `MOCK`. A operação financeira real permanece bloqueada. O futuro provedor deve substituir o adaptador, sem mover regras de pedido, reserva, snapshots ou idempotência para um controller.
## Ponto de substituição
Implementar `PaymentProvider` em `com.perfumestore.backend.payment` e ativar a implementação por perfil/configuração. A interface deve criar uma sessão hospedada e consultar o estado de um pagamento. O adaptador não decide o total: recebe o valor e a moeda já persistidos no pedido.
O componente deve:
- gerar ou reutilizar uma chave externa idempotente;
- retornar identificadores externos e expiração;
- não armazenar cartão ou credenciais do cliente;
- representar timeout e resultado desconhecido como estado reconciliável;
- nunca confirmar estoque diretamente.
## Webhooks oficiais

O controller futuro deve separar recebimento, autenticação, validação estrutural, persistência do evento e processamento. O HMAC do MOCK não é contrato do provedor real. Deve ser utilizado o mecanismo exigido pela documentação oficial escolhida, com validação dos bytes exatos, replay protection e segredo externo ao código.

O identificador do evento externo deve continuar protegido por unicidade persistida. Eventos repetidos ou fora de ordem devem reutilizar `PaymentService.processWebhook`, mantendo a máquina de estados e os efeitos idempotentes.

## Estados e reconciliação

Mapear estados externos para `PaymentAttemptStatus` e `PaymentStatus` internos sem regredir terminais válidos. Aprovação, valor e moeda precisam corresponder à tentativa. A reconciliação deve consultar o provedor em pagamentos `PENDING` ou `UNKNOWN`, registrar a tentativa e reutilizar o mesmo processamento dos eventos.

Se uma aprovação chegar após uma reserva expirar, preservar a aprovação financeira e manter o pedido em `PAYMENT_REVIEW`; não recriar reserva automaticamente nem autorizar entrega.

## Configuração e produção

Adicionar somente propriedades externas para URLs, chaves e timeouts do provedor escolhido. Produção deve falhar de forma controlada quando o provedor não estiver configurado. É proibido usar MOCK como fallback, publicar endpoints de simulação ou deixar segredos em migrations, logs, `.env.example` ou respostas.

## URLs e retorno hospedado

As URLs de retorno devem ser allowlisted por ambiente, não derivadas do header `Host` e não devem ser usadas como confirmação financeira. O retorno do navegador apenas orienta a interface; o estado comercial continua dependendo de evento autenticado ou reconciliação.

## Checklist antes da liberação

1. Escolher o provedor e revisar contrato jurídico/comercial.
2. Criar credenciais sandbox fora do repositório.
3. Implementar o adaptador real e o mapeamento de estados.
4. Implementar assinatura/autenticação oficial de webhook.
5. Testar valor, moeda, idempotência, duplicidade, ordem e replay.
6. Testar timeout, consulta e reconciliação.
7. Homologar aprovação tardia e `PAYMENT_REVIEW`.
8. Confirmar que o perfil `prod` não inicializa MOCK.
9. Executar testes de integração e uma homologação financeira controlada.
10. Somente então avaliar a liberação comercial.
