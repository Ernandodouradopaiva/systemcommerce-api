# Prompts 126–129 — Partes, motor, operações e DFe base

| Prompt | Escopo | Migration |
|---|---|---|
| 126 | `party_fiscal_profiles` | V257 |
| 127 | Motor `tax_rules` / `tax_calculations` | V258 |
| 128 | `fiscal_operations` | V259 |
| 129 | `fiscal_documents` + satélites | V260 |
| Perms | `FISCAL_PARTY_*`, `FISCAL_TAX_*`, `FISCAL_OPERATION_*`, `FISCAL_DOCUMENT_*` | V261 |

## Fronteiras

- Motor **não** importa `nfe`/`nfce`.
- Operação fiscal **orienta** estoque/financeiro; não escreve `stock_movements`.
- DFe autorizado é **imutável**; numeração com lock; XML com SHA-256.
- Simulação não cria documento fiscal de emissão.

## Front

- Aba Fiscal em Cliente e Fornecedor
- Menu: Operações, Simulador, Documentos fiscais
