# Integração financeira com compras (Prompt 102)

## Fluxo

```
Pedido de compra → Recebimento (aceite/post) → Validação doc. fornecedor
        → Conta a pagar (parcial ou total) → Parcelas → Pagamento
```

## Configuração (`finance_generation_settings`)

| Modo (`payable_generation_mode`) | Quando gera |
|----------------------------------|---------------|
| `ON_RECEIPT` (**padrão**) | Após post do recebimento no estoque |
| `ON_ORDER_APPROVED` | Na aprovação do pedido |
| `ON_INVOICE_ENTRY` | Entrada de nota (flag dedicada) |
| `MANUAL` | Somente endpoint / tela |

Frete: `INCORPORATED` (padrão) ou `SEPARATE`. Impostos: `segregate_taxes`.

API: `GET/PUT /api/v1/finance-generation-settings`

## Regras implementadas

- Anti-duplicidade por `(origin_type, origin_document_id)` + idempotency key
- Valor da AP = qty aceita × custo unitário do recebimento (parcial permitido)
- Diferença pedido × recebido registrada nas notes
- Frete/impostos proporcionais ao ratio parcial; podem ser segregados
- Devolução concluída gera crédito/ajuste (`SUPPLIER_RETURN`)
- Cancelamento analisa pagamentos (`/payables/{id}/cancel-analysis`)
- Fornecedor e loja preservados do recebimento
- Conta referencia o recebimento na origem
