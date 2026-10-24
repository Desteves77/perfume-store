# Configurações históricas

Os arquivos abaixo foram preservados para permitir a reprodução das homologações anteriores. Eles não devem ser usados como entrada padrão da loja:

| Arquivo | Finalidade | Situação |
| --- | --- | --- |
| `compose.catalog-acceptance.yaml` | Catálogo/Fase 4.1 e estoques iniciais | Histórico |
| `compose.phase6-acceptance.yaml` | Endereços e frete/Fase 6 | Histórico |
| `compose.phase7-acceptance.yaml` | Carrinho/Fase 7 | Histórico |
| `compose.phase10-acceptance.yaml` | Pós-venda/Fase 10 | Histórico |
| `compose.phase11-acceptance.yaml` | Notificações/Fase 11 | Histórico |
| `compose.phase12-acceptance.yaml` | Homologação backend/Fase 12 | Preservado e ainda utilizável |
| `compose.phase13-acceptance.yaml` | Homologação integrada/Fase 13 | Preservado e ainda utilizável |

Os arquivos continuam na raiz porque os relatórios históricos registram esses caminhos. Nenhum container, volume ou banco é removido por esta organização. A classificação não implica que um recurso parado seja descartável.

Os exemplos `phase*-acceptance.env.example` seguem a mesma regra: são configurações sintéticas e isoladas, não credenciais produtivas.
