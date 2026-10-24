# Frontend — arquitetura

O frontend da Fase 13 fica isolado em `frontend/` e utiliza Next.js 15.5.28, React 19.1, TypeScript 5.9, Tailwind CSS 3.4 e um design system CSS tokenizado. A aplicação usa App Router e componentes client-side apenas onde há interação ou sessão.

`src/lib/api.ts` centraliza URL, cookies de sessão, CSRF por `X-XSRF-TOKEN`, `application/problem+json`, `X-Correlation-Id` recebido pelo backend e `Idempotency-Key`. O navegador nunca persiste a sessão em localStorage. `SessionProvider` consulta `/api/v1/auth/me` e o backend continua sendo a autoridade de autorização.

As áreas são separadas em loja pública, conta do cliente e painel administrativo. O `AdminShell` oculta a operação para perfis que não sejam ADMIN, mas cada chamada permanece protegida pelo backend.

O frontend não possui banco, estoque, carrinho comercial ou máquina de estados própria. Valores são formatados para apresentação com base nos DTOs do backend; checkout, frete, disponibilidade, pagamento e pós-venda são confirmados pela API.

Na homologação integrada, o navegador acessa uma única origem HTTPS em `https://localhost:19680`. O Caddy encaminha a aplicação para o Next.js e `/api/*` para o Spring Boot. Essa topologia mantém cookies e CSRF na mesma origem e é exclusiva do ambiente isolado.
