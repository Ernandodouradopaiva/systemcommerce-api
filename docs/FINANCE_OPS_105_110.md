# Operações financeiras avançadas (Prompts 105–110)

## Migrations

| Versão | Conteúdo |
|--------|----------|
| V235 | Adiantamentos (`customer_advances`, `supplier_advances`, applications, refunds) |
| V236 | Políticas de encargos + `settlement_adjustments` |
| V237 | Transferências entre holders |
| V238 | Lançamentos manuais |
| V239 | Reversões / estornos centrais |
| V240 | Renegociações |
| V241 | Permissões |

## Módulos

### 105 — Adiantamentos
- Endpoints: `/api/v1/customer-advances`, `/supplier-advances`, `/advance-applications`, `/advance-refunds`
- Gera movimento no holder; saldo só na API; aplicação parcial; cancelamento com estorno

### 106 — Políticas
- `/api/v1/financial-charge-policies` + `POST .../simulate`
- Juros simples diários, multa fixa/% , desconto antecipação; vigência/prioridade; simulação sem efetivar

### 107 — Transferências
- `/api/v1/financial-transfers` (draft → confirm → reverse)
- `TRANSFER_OUT` + `TRANSFER_IN` atômicos; tarifa opcional; origem ≠ destino

### 108 — Lançamentos manuais
- `/api/v1/financial-entries`
- Tipos: receita/despesa/ajuste/tarifa/rendimento/imposto/correção/saldo inicial
- Confirmado imutável; estorno formal; permissões `FINANCIAL_ENTRY_*`

### 109 — Reversões
- `/api/v1/financial-reversals`
- Não apaga confirmados; gera movimento inverso; anti-duplicidade por origem

### 110 — Renegociação
- `/api/v1/financial-renegotiations`
- Seleciona parcelas abertas; aplica encargos; gera nova condição; marca originais como renegociadas
