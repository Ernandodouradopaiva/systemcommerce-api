# Integração financeira do PDV (Prompt 104)

## Princípio

A **sessão de caixa** confere o dinheiro físico. O **holder financeiro** (caixa PDV / conta PIX / adquirente) registra o saldo oficial. Não se deve somar os dois como se fossem o mesmo movimento.

`CashMovement.financial_holder_movement_id` vincula sangria/suprimento ao `FinancialHolderMovement` correspondente.

## Fluxos

### Dinheiro

```
Venda PDV → Receivable (origem POS) → Liquidação no holder do caixa POS
         → CashMovement CASH_SALE (gaveta física — conferência)
```

### PIX

```
Venda PDV → Receivable → Liquidação no holder PIX configurado (`pos_pix_holder_id`)
         ou fallback no caixa POS da loja
```

### Cartão (débito/crédito)

```
Venda PDV → Receivable permanece em aberto (previsão adquirente)
         → Liquidação futura / ou imediata se `settle_pos_card_immediately=true`
```

### Pagamento dividido

Cada meio confirmado é avaliado independentemente; liquidações são agrupadas por holder.

### Sangria / Suprimento

```
CashMovement WITHDRAWAL/SUPPLY → FinancialHolderMovement TRANSFER_OUT/IN (mesmo valor)
```

### Fechamento

Fecha a sessão e reconcilia a gaveta. **Não** recria vendas nem AR.

### Cancelamento / estorno

Estorna pagamentos e `CashMovement` de refund. AR aberta é cancelada; AR já recebida exige estorno de liquidação.

## Configuração relevante

- `generate_and_settle_pos_cash`
- `settle_pos_cash` / `settle_pos_pix` / `settle_pos_card_immediately`
- `pos_pix_holder_id` / `pos_card_acquirer_holder_id`

## Serviço

`PosFinanceIntegrationService` — chamado em `PosCheckoutService.finalize` e em sangria/suprimento.
