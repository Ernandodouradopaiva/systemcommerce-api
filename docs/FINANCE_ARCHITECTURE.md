# Arquitetura financeira — cadastros mestres (Prompts 92–95)

## Princípio

Cadastros financeiros **não** substituem documentos comerciais. Contas a pagar/receber e liquidações virão em prompts seguintes, sempre a partir de compra/venda faturada.

Separação obrigatória:

| Conceito | Entidade | Uso |
|---|---|---|
| Plano de contas | `FinancialAccount` | Contábil / categorização |
| Categoria financeira | `FinancialCategory` | Vínculo compras/vendas → conta analítica |
| Centro de custo | `CostCenter` | Dimensão gerencial / rateio (preparado) |
| Instrumento operacional | `FinancialAccountHolder` | Conta corrente, caixa, carteira |
| Conta bancária | `BankAccount` | Detalhe do holder bancário |
| Caixa financeiro | `FinancialCash` | Caixa admin/PDV (paralelo a `CashSession`) |
| Forma/condição | `PaymentMethod` / `PaymentCondition` | Catálogo (tabela `fin_payment_methods`) |

## Migrations

- `V224` plano de contas + hierarquia + categorias
- `V225` centros de custo + assignments + `requires_cost_center`
- `V226` bancos, holders, bank_accounts, financial_cashes, payment_accounts, movements
- `V227` formas/condições/parcelas/config por loja
- `V228` permissões + seeds ORG-DEFAULT

## Regras

- Código único por organização
- Hierarquia sem ciclos (closure table)
- Sintética não recebe lançamento; analítica recebe
- Sem exclusão física; inativação preserva histórico
- Saldo do holder = soma de `financial_holder_movements` (saldo inicial gera `OPENING_BALANCE`)
- Percentuais de parcelas = 100% (API calcula vencimentos)

## Endpoints (prefixo `/api/v1`)

- `financial-accounts` (+ `/tree`, `/postable`, `/{id}/reorganize`, `/{id}/history`)
- `financial-categories`
- `cost-centers` (+ `/tree`, `/{id}/stores`, `/{id}/history`)
- `banks`, `bank-accounts` (+ `/{id}/balance`), `financial-cashes`, `payment-accounts`
- `payment-methods`, `payment-conditions` (+ `/{id}/calculate-due-dates`)

## Front

Rotas: `/financial-accounts`, `/cost-centers`, `/bank-accounts`, `/payment-methods`, `/payment-conditions`  
Grupo de menu: **Financeiro**

## Continuação (Prompts 96–101)

Ver [`FINANCE_AP_AR.md`](./FINANCE_AP_AR.md).

PDV à vista: `PosFinanceIntegrationService` gera a AR e liquida conforme o meio de pagamento.

## Integrações (Prompts 102–104)

- Compras: [`FINANCE_PURCHASE_INTEGRATION.md`](./FINANCE_PURCHASE_INTEGRATION.md)
- Vendas: [`FINANCE_SALES_INTEGRATION.md`](./FINANCE_SALES_INTEGRATION.md)
- PDV: [`PDV_FINANCIAL_INTEGRATION.md`](./PDV_FINANCIAL_INTEGRATION.md)
- Migration `V234` — modos de geração, frete/imposto, vínculo CashMovement ↔ holder
