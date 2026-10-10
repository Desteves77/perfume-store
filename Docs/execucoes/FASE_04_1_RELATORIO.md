# Fase 4.1 — Relatório de homologação isolada do catálogo

## Estado inicial

- Diretório: `C:\Users\User\Desktop\Ecommerce PerfumeStore`.
- HEAD inicial: `cde808000f35328edddc8799b85e1c1ee29b083b` (`feat: implement catalog categories presentations and images`).
- A árvore de trabalho estava limpa no início da execução.
- Não havia `AGENTS.md` no diretório raiz.
- O PostgreSQL de desenvolvimento existente em `127.0.0.1:54329` foi identificado e preservado. Nenhuma credencial existente foi lida.
- A implementação anterior contém as migrations V1–V3, catálogo, imagens locais, sessões, CSRF e provisionamento dedicado de ADMIN.

## Plano

1. Criar arquivos próprios para Compose, banco, volume, diretório de imagens, Mailpit e porta HTTP.
2. Validar a configuração e iniciar somente os serviços isolados.
3. Provisionar um ADMIN sintético pelo mecanismo real e verificar idempotência/encerramento.
4. Executar smoke HTTP administrativo, público, de permissões e CSRF.
5. Reiniciar aplicação e PostgreSQL sem remover o volume e conferir persistência.
6. Revisar a cobertura automatizada, executar `clean verify`, atualizar documentação e criar commit local.

## Recursos isolados planejados

- Projeto Compose: `perfumestore-catalog-acceptance`.
- PostgreSQL: projeto e volume próprios, porta local `56000`.
- Mailpit: SMTP `11025`, interface `18025`.
- Aplicação: porta local `18080`.
- Imagens: `acceptance-data/catalog-images`, fora do Git.
- Credenciais: geradas somente para esta execução e mantidas no arquivo local ignorado `.env.catalog-acceptance`.

Nenhum segredo, cookie, token ou senha será registrado neste documento.

## Execução

Os resultados, códigos de saída e cenários serão acrescentados durante a homologação. Logs longos permanecerão em `Docs/execucoes/logs/`, ignorado pelo Git.

## Registro durante a execução

## Decisões e justificativas

- Foi usado um projeto Compose separado, sem `container_name` fixo, para evitar colisão com o ambiente de desenvolvimento.
- A porta `56000` foi escolhida após `55432` e `55433` serem recusadas pelo Windows por intervalos de exclusão de portas. O banco de desenvolvimento em `54329` não foi parado nem alterado.
- O ADMIN foi criado pelo runner real com perfil `local,provision-admin`, `SERVER_PORT=0` e credenciais sintéticas carregadas somente no ambiente do processo. Não houve inserção SQL nem exposição de senha.
- O smoke usou clientes HTTP independentes para ADMIN, CUSTOMER e anônimo, mantendo CSRF e sessões reais.
- O diretório de imagens foi um diretório persistente fora do classpath e do Git. Os arquivos foram verificados por SHA-256 após a reinicialização.

## Problemas encontrados e correções

1. As portas iniciais `55432` e `55433` falharam no bind por exclusão do Windows. O Compose parcial deste próprio projeto foi encerrado sem `-v`, a porta `56000` foi configurada e o volume dedicado foi preservado.
2. O script PowerShell de smoke tinha uma interpolação inválida (`$label:`) e depois misturava mensagens de diagnóstico ao valor retornado do token CSRF. Ambos foram corrigidos no script local de execução; não houve alteração funcional na aplicação.
3. A primeira consulta de contagem usou `perfumestore.users`, mas a migration define `perfumestore.app_users`. A verificação foi corrigida para a tabela real e confirmou um ADMIN.

## Resultado do provisionamento

- Primeira execução: código `0`.
- Segunda execução com as mesmas variáveis: código `0`.
- Contagem final no banco isolado: `1` ADMIN.
- O processo dedicado encerrou após a operação e a porta HTTP efêmera não permaneceu aberta.
- Não houve promoção de CUSTOMER nem redefinição silenciosa.

## Resultado do smoke HTTP

O fluxo completo passou: health `200`, login e perfil ADMIN, criação de categoria/produto/apresentações, dois uploads PNG, definição de principal, reordenação, publicação, consulta pública, busca, filtro, alteração de preço, ocultação de apresentação inativa, remoção de imagem não principal, validação de CSRF, bloqueio de anônimo/CUSTOMER, desativação do produto, `404` público e preview administrativo `200`.

Os identificadores do fluxo aprovado, mantidos apenas como dados sintéticos de rastreio, foram: categoria `cb27312d-df16-4418-baf4-5a48dc97f04f`, produto `552b786f-3bde-4ea0-9872-0d1f5fd45f5f`, apresentações `6b82a319-8a72-4b9b-88fc-216e72562ffe` e `b99ee597-c3ec-49cb-b40e-060386837759`, imagens `fa37a650-429e-467e-b27e-bc42d548420f` e `b6167a03-a979-4f57-a69a-580e78c0975d`.

## Persistência

Após reiniciar o container PostgreSQL dedicado e a aplicação, o produto permaneceu inativo com versão `2`; a apresentação de 50 ml permaneceu ativa com preço `249.90`; a de 100 ml permaneceu inativa; a imagem `b6167a03-a979-4f57-a69a-580e78c0975d` permaneceu principal na posição `0`; a outra imagem permaneceu logicamente removida na posição `1`. O catálogo público continuou retornando `404` para o produto inativo e o preview ADMIN retornou `200`.

O Flyway registrou `BASELINE`, `1`, `2` e `3` com sucesso. Os arquivos sintéticos existentes no diretório de homologação mantiveram o SHA-256 `431CED6916A2A21A156E38701AFE55BBD7F88969FBBFC56D7FE099D47F265460` após a reinicialização.

## Cobertura e testes adicionais

`CatalogIntegrationTest` passou de 4 para 6 testes. Os dois novos cenários cobrem escala monetária inválida e atualização com `version` obsoleta, além do efeito de desativar categoria sobre a publicação. A tabela detalhada de lacunas e evidências está em `FASE_04_1_TESTES.md`.

## Estado final

A homologação isolada foi concluída. O ambiente de homologação foi parado ao final sem remover o volume PostgreSQL nem o diretório `acceptance-data/catalog-images`; o ambiente de desenvolvimento permaneceu preservado. A Fase 4 está validada localmente, e a Fase 5 não foi iniciada.
