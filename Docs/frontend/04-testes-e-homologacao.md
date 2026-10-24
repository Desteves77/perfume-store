# Frontend — testes e homologação

Comandos do frontend:

```powershell
npm ci
npm run typecheck
npm run lint
npm test
npm run build
npm run test:e2e
```

Vitest cobre formatação BRL e rótulos de estados. Playwright cobre a navegação pública e, quando `NEXT_PUBLIC_API_URL` aponta para a homologação, consulta o endpoint público de catálogo do backend real.

Na execução da Fase 13, o smoke Playwright passou com dois cenários contra o frontend e o catálogo HTTPS da homologação (`https://localhost:18680`). Os demais fluxos comerciais dependem de dados e credenciais sintéticos do ambiente isolado; não são declarados como homologação comercial real.

Para validação visual, revisar 375px, 768px e 1440px, teclado, foco, contraste, textos alternativos e ausência de rolagem horizontal indevida. Artefatos temporários de Playwright não pertencem ao commit.

Na continuação da Fase 13, `npm run typecheck`, `npm run lint`, `npm test`, `npm run build` e `npm run test:e2e` passaram. O Playwright executou 5 testes: jornada integrada CUSTOMER/ADMIN, recuperação de senha via Mailpit, segurança/isolamento incluídos no fluxo, navegação pública e verificação mobile. O backend `clean verify` passou com 82 testes em PostgreSQL real.
