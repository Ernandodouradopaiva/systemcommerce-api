# Integração financeira com vendas (Prompt 103)

## Fluxo

```
Orçamento → Pedido de venda → Faturamento → Conta a receber → Recebimento
```

## Regras

| Etapa | Financeiro |
|-------|------------|
| Orçamento | Não gera |
| Pedido | Não gera AR definitiva por padrão |
| Faturamento | Gera AR (config `generate_receivable_on_invoice`) |
| Condição de pagamento | Define parcelas (quando informada) |
| Adiantamento | Origem `ADVANCE` disponível |
| Faturamento parcial | AR pelo valor da venda gerada |
| Cancelamento pedido (não faturado) | Sem AR |
| Cancelamento venda / PDV | Cancela AR aberta; se recebida → exige estorno/crédito |
| Idempotência | `organization_id` + `idempotency_key` e unique de origem |

Origens registradas no faturamento: `SALE`/`POS` (documento) + `SALES_ORDER` (pedido).

Vendedor, loja, cliente e número do documento são preservados a partir da venda.
