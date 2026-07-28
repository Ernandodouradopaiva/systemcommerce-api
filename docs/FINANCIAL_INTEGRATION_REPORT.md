# Relatório de integração financeira (Prompts 102–104)

> Regra definitiva de fronteira entre estoque e financeiro no SystemCommerce.

---

## Regra de integração

```
Estoque    = quantidades  (entrada/saída/reserva por documento logístico/comercial)
Financeiro = valores      (títulos, parcelas, liquidações, movimentos em holders)
Fiscal     = DFe          (XML + SEFAZ; não regenera estoque nem títulos)
Vínculo    = documentos comerciais (pedido, recebimento, venda faturada, venda PDV)
```

O módulo financeiro **nunca** altera saldo de estoque. O módulo fiscal **nunca** altera estoque nem gera Payable/Receivable. Eventos de estoque (post de recebimento, faturamento de venda) **disparam** ou **habilitam** geração financeira conforme `finance_generation_settings`. Valores financeiros derivam dos totais comerciais já confirmados na API comercial. Detalhe fiscal: [`FISCAL_INTEGRATION.md`](./FISCAL_INTEGRATION.md).

---

## Compras

**Gatilho padrão:** recebimento postado no estoque (`POSTED_TO_INVENTORY`).

```
Pedido de compra → Recebimento (aceite/post) → Payable (+ parcelas) → PayableSettlement → FinancialHolderMovement
```

- Valor AP = quantidade aceita × custo unitário do recebimento (parcial permitido)
- Anti-duplicidade: `PayableOrigin` tipo `PURCHASE_RECEIPT`
- Fornecedor e loja herdados do recebimento
- Estoque já movimentado pelo módulo de compras **antes** da geração AP

Detalhes: [`FINANCE_PURCHASE_INTEGRATION.md`](./FINANCE_PURCHASE_INTEGRATION.md)

---

## Vendas ERP

**Gatilho padrão:** faturamento (`generate_receivable_on_invoice`).

```
Orçamento → Pedido → Faturamento → Receivable (+ parcelas) → ReceivableSettlement → FinancialHolderMovement
```

- Orçamento e pedido não faturado: **sem** AR definitiva
- Faturamento parcial: AR proporcional ao valor faturado
- Cancelamento: cancela AR aberta; AR recebida exige estorno formal
- Anti-duplicidade: `ReceivableOrigin` tipos `SALE`, `SALES_ORDER`

Detalhes: [`FINANCE_SALES_INTEGRATION.md`](./FINANCE_SALES_INTEGRATION.md)

---

## PDV

**Princípio:** sessão de caixa (gaveta física) ≠ holder financeiro (saldo oficial).

```
Venda PDV → Receivable (origem POS) → Liquidação no holder do meio de pagamento
         → CashMovement (conferência física, não somar ao holder como movimento duplicado)
```

- Dinheiro: liquidação no caixa POS + `CashMovement CASH_SALE`
- PIX/cartão: holders dedicados conforme config (`pos_pix_holder_id`, adquirente)
- Sangria/suprimento: `CashMovement` vinculado a `FinancialHolderMovement` via `financial_holder_movement_id` (migration `V234`)
- Fechamento de sessão: reconcilia gaveta; **não** recria vendas nem AR

Detalhes: [`PDV_FINANCIAL_INTEGRATION.md`](./PDV_FINANCIAL_INTEGRATION.md)

---

## Configuração central

| Recurso | Endpoint / entidade |
|---------|---------------------|
| Modos de geração AP/AR | `finance_generation_settings` — `GET/PUT /api/v1/finance-generation-settings` |
| Liquidação PDV | flags `settle_pos_cash`, `settle_pos_pix`, `settle_pos_card_immediately`, etc. |
| Vínculo caixa PDV | coluna `cash_movements.financial_holder_movement_id` |

---

## Serviços de integração

| Domínio | Serviço principal |
|---------|-------------------|
| Compras | `PayableService.generateFromPurchaseReceipt` |
| Vendas | `ReceivableService.generateFromSale` |
| PDV | `PosFinanceIntegrationService` (checkout, sangria, suprimento) |
| Migração legado | `FinanceBackfillService` (Prompt 120) |

---

## Documentação complementar

- AR/AP e liquidações: [`FINANCE_AP_AR.md`](./FINANCE_AP_AR.md)
- Arquitetura de cadastros: [`FINANCE_ARCHITECTURE.md`](./FINANCE_ARCHITECTURE.md)
- Validação: [`FINANCIAL_VALIDATION_REPORT.md`](./FINANCIAL_VALIDATION_REPORT.md)
