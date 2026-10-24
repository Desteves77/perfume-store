# Frontend — design system

O sistema visual usa uma única linguagem: fundo claro, neutros quentes, acento cobre, tipografia serifada nos títulos e sans-serif no texto funcional. Os tokens ficam em `src/app/globals.css` e Tailwind está configurado para extensões futuras sem substituir o CSS de componentes já homologado.

Componentes reutilizáveis: `Shell`, `AdminShell`, `Button`, `Field`, `Select`, `Panel`, `Notice`, `ErrorNotice`, `StatusBadge`, `Spinner` e `ProductImage`. Estados de carregamento, vazio, erro, sucesso e falta de permissão são tratados nas jornadas principais.

Os formulários usam labels, foco visível, `aria-invalid`, mensagens de erro e HTML semântico. O layout usa grids responsivos para 375px, 768px e desktop, com tabelas em contêineres de rolagem horizontal e navegação mobile.

A validação de homologação confirmou os fluxos de 375px, 768px e 1440px, ausência de overflow horizontal e textos alternativos nas imagens principais. A interface mantém estados de carregamento, sucesso, erro, vazio e falta de permissão nos fluxos executados.
