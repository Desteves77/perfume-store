# Frontend — operação e ambientes

Para a homologação integrada, use `compose.phase13-acceptance.yaml` com `phase13-acceptance.env.example`. As portas sintéticas são: HTTPS `19680`, HTTP de redirecionamento `19681`, PostgreSQL `56610`, SMTP Mailpit `11435` e interface Mailpit `18635`. O PostgreSQL e os volumes são exclusivos da homologação; não use `down -v`.

`frontend/.env.example` contém apenas `NEXT_PUBLIC_API_URL`. Não versionar `.env.local`, credenciais, certificados ou tokens.

Desenvolvimento usa o backend local e pode utilizar Mailpit, frete sintético e pagamento MOCK somente em ambiente autorizado. Homologação pode apontar para o proxy HTTPS da Fase 12. Produção exige URL, CORS, CSRF, cookies e integrações reais explicitamente configurados pelo backend; o frontend não cria fallback para MOCK, Mailpit ou frete sintético.

O build é gerado com `npm run build` e executado com `npm start`. Para uma verificação HTTPS local do próprio frontend, usar `npm run dev:https -- -p 3101`; o certificado autogerado é somente de homologação. A implantação pública não faz parte da Fase 13. Antes dela, revisar domínio, proxy HTTPS, política de cache para dados privados, SMTP, provedor financeiro, frete, storage de imagens e decisões comerciais.
