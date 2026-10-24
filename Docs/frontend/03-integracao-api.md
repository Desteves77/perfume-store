# Frontend — integração com a API

`NEXT_PUBLIC_API_URL` define a origem do backend; o exemplo local usa `http://localhost:8080`. Para a homologação da Fase 12 foi usado `https://localhost:18680`.

Todas as mutações fazem uma leitura de CSRF em `/api/v1/security/csrf` e enviam o token retornado em `X-XSRF-TOKEN`, sempre com `credentials: include`. O cookie de sessão permanece HttpOnly e sob controle do backend.

Contratos TypeScript em `src/lib/types.ts` refletem `Money`, `Page`, catálogo, carrinho, endereço, cotação, pedido, pagamento e recursos administrativos. Problemas são convertidos em `ApiError` e exibidos sem stack trace. A aplicação não aceita preço, frete, total ou estado comercial como autoridade do navegador.

Imagens usam as URLs retornadas pela API. O cliente não habilita destinos arbitrários, não cria JWT, não coloca sessão em localStorage e não usa rotas Next como substituição da autorização Java.

Durante a integração foi corrigido o consumo de endereços: `GET /api/v1/me/addresses` é paginado e o frontend passou a consumir `Page<Address>.items`. O painel passou a usar o contrato de publicação de produto e os endpoints administrativos reais de recebimento, inspeção e reposição.
