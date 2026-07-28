# Contas a pagar / receber (Prompts 96–101)

## Fluxo

```
Compra postada → Payable (configurável) → Parcelas → PayableSettlement → FinancialHolderMovement (PAYMENT)
Pedido faturado / PDV → Receivable (configurável) → Parcelas → ReceivableSettlement → FinancialHolderMovement (RECEIPT)
```

## Migrations

- `V229` payables + installments + origins + allocations + history + `finance_generation_settings`
- `V230` payable settlements
- `V231` receivables + installments + origins
- `V232` receivable settlements
- `V233` permissões + seed settings ORG-DEFAULT

## Regras centrais

- Sem exclusão física; conta paga/recebida não edita direto
- Duplicidade bloqueada por `(origin_type, origin_document_id)`
- Totais e saldos **só na API**
- Liquidação atômica: lock parcela → settlement → movimento holder → atualiza status
- Idempotência em criação e liquidação
- Geração automática configurável em `finance_generation_settings`
- PDV à vista: gera AR e liquida automaticamente quando há holder de caixa POS resolvido

## Endpoints

- `/api/v1/payables` (+ agenda, balance, installments, history, from-purchase-receipt, cancel, renegotiate)
- `/api/v1/payable-settlements` (+ confirm)
- `/api/v1/receivables` (+ agenda, by-customer, from-sale, write-off, …)
- `/api/v1/receivable-settlements` (+ confirm)

## Front

`/payables`, `/payables/agenda`, `/receivables`, `/receivables/agenda`
