# Prompts 138–142 — CC-e, contingência, especiais, devoluções e entrada

| Prompt | Entrega | Migration |
|---|---|---|
| 138 | CC-e (só NF-e 55) | V267 |
| 139 | Contingência + [FISCAL_CONTINGENCY_RUNBOOK.md](./FISCAL_CONTINGENCY_RUNBOOK.md) | V268 |
| 140 | Complementar/ajuste/remessa/retorno | (emissão especial) |
| 141 | Devoluções fiscais vinculadas | V269 |
| 142 | Entrada XML imutável | V270 |
| Perms | | V271 |

## Regras-chave

- CC-e **não** se aplica à NFC-e; original permanece imutável.
- Contingência **não** por rejeição fiscal; consultar protocolo antes de retransmitir.
- Especial exige referência e permissão `FISCAL_SPECIAL_EMIT`.
- Devolução fiscal **não** duplica estoque (módulos comerciais).
- XML de entrada nunca é alterado; estoque só via recebimento.
