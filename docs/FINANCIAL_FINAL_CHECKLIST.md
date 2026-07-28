# Checklist final financeiro (Prompts 91–120)

> Critérios de gate do Prompt 120: **não declarar concluído** se houver duplicatas, saldos inconsistentes, estoque alterado pelo financeiro ou cálculos no front-end.

**Legenda:** ✅ OK · ⚠️ Pendência documentada · ❌ Bloqueador

---

## Prompts 91–95 — Cadastros mestres (`V224`–`V228`)

| # | Item | Status | Referência |
|---|------|--------|------------|
| 91 | Separação plano de contas × holders × documentos comerciais | ✅ | [`FINANCE_ARCHITECTURE.md`](./FINANCE_ARCHITECTURE.md) |
| 92 | Plano de contas hierárquico (`FinancialAccount`) | ✅ | `V224`, `financial-accounts` API |
| 93 | Centros de custo (`CostCenter`) | ✅ | `V225`, `cost-centers` API |
| 94 | Bancos, holders, contas, caixas financeiros | ✅ | `V226`, `BankFinanceController` |
| 95 | Formas/condições de pagamento por loja | ✅ | `V227`–`V228`, seeds ORG-DEFAULT |

---

## Prompts 96–101 — AP/AR (`V229`–`V233`)

| # | Item | Status | Referência |
|---|------|--------|------------|
| 96 | Payables + parcelas + origens | ✅ | `V229`, `PayableService` |
| 97 | Liquidações AP (`PayableSettlement`) | ✅ | `V230` |
| 98 | Receivables + parcelas + origens | ✅ | `V231` |
| 99 | Liquidações AR (`ReceivableSettlement`) | ✅ | `V232` |
| 100 | Idempotência e anti-duplicidade | ✅ | `ReceivableOrigin` / `PayableOrigin` |
| 101 | Permissões AP/AR + settings | ✅ | `V233`, [`FINANCE_AP_AR.md`](./FINANCE_AP_AR.md) |

---

## Prompts 102–104 — Integração comercial (`V234`)

| # | Item | Status | Referência |
|---|------|--------|------------|
| 102 | Compra → AP automática configurável | ✅ | [`FINANCE_PURCHASE_INTEGRATION.md`](./FINANCE_PURCHASE_INTEGRATION.md) |
| 103 | Venda faturada → AR | ✅ | [`FINANCE_SALES_INTEGRATION.md`](./FINANCE_SALES_INTEGRATION.md) |
| 104 | PDV → AR + liquidação + vínculo caixa | ✅ | [`PDV_FINANCIAL_INTEGRATION.md`](./PDV_FINANCIAL_INTEGRATION.md), `V234` |

---

## Prompts 105–110 — Operações (`V235`–`V241`)

| # | Item | Status | Referência |
|---|------|--------|------------|
| 105 | Adiantamentos cliente/fornecedor | ✅ | `AdvanceController` |
| 106 | Políticas juros/multa/desconto | ✅ | `FinancialChargePolicyServiceTest` |
| 107 | Transferências entre holders | ✅ | `FinancialTransferService` + aprovação |
| 108 | Lançamentos manuais | ✅ | `FinancialEntryController` |
| 109 | Estornos formais | ✅ | `FinancialReversalController` |
| 110 | Renegociação de parcelas | ✅ | `FinancialRenegotiationController` |

---

## Prompts 111–113 — Conciliação e cobrança (`V242`–`V245`)

| # | Item | Status | Referência |
|---|------|--------|------------|
| 111 | Conciliação bancária OFX/CSV | ✅ | [`FINANCE_RECONCILIATION_CARD_BILLING.md`](./FINANCE_RECONCILIATION_CARD_BILLING.md) |
| 112 | Cartões/adquirentes/previsão | ✅ | `CardAcquirerController` |
| 113 | Boletos/PIX/cobranças | ✅ | `BillingController` |

---

## Prompts 114–118 — Analytics (`V246`–`V249`)

| # | Item | Status | Referência |
|---|------|--------|------------|
| 114 | Fluxo de caixa realizado/projetado | ✅ | `CashFlowController` |
| 115 | Fechamento e períodos financeiros | ✅ | `FinancialClosingController`, `FinancialPeriodGuard` |
| 116 | DRE gerencial | ✅ | `IncomeStatementController` |
| 117 | Dashboard financeiro | ✅ | `FinanceDashboardController` |
| 118 | Relatórios e exportação auditada | ✅ | `FinanceReportController`, `V248` |

---

## Prompts 119–120 — Segurança e migração (`V250`–`V251`)

| # | Item | Status | Referência |
|---|------|--------|------------|
| 119 | Política e solicitações de aprovação em duas etapas | ✅ | [`FINANCIAL_SECURITY_REPORT.md`](./FINANCIAL_SECURITY_REPORT.md) |
| 119 | Permissões granulares + auditoria financeira | ✅ | `V251`, `FinanceAuditEvents` |
| 119 | Sanitização de auditoria (sem segredos) | ✅ | `AuditSanitizer` |
| 120 | Backfill idempotente AR/AP | ✅ | [`FINANCIAL_MIGRATION_PLAN.md`](./FINANCIAL_MIGRATION_PLAN.md) |
| 120 | Registro de execuções (`finance_migration_runs`) | ✅ | `V250` |

---

## Critérios de gate (Prompt 120)

| Critério | Verificação |
|----------|-------------|
| ❌ Duplicatas AR/AP | Consultar `receivable_origins` / `payable_origins`; reexecutar backfill em dry-run |
| ❌ Saldos holders inconsistentes | Reconciliar `/bank-accounts/{id}/balance` vs movimentos |
| ❌ Estoque mutado por financeiro | Confirmar ausência de chamadas inventory nos serviços `finance/*` |
| ❌ Cálculos no front-end | Front apenas exibe valores da API |
| ❌ Lançamentos sem origem comercial (exceto manuais) | Auditar títulos `MANUAL_*` vs integrados |
| ❌ Aprovação pelo solicitante | Teste `FinancialApprovalService.decide` rejeita mesmo usuário |

**Declarar concluído somente se todos os critérios acima passarem.**

---

## Comandos obrigatórios

```bash
# Backend
cd SystemCommerce-api && mvn clean verify

# Frontend
cd SystemCommerce-front && npm run lint && npm run test && npm run build
```

---

## Matriz de permissões — Prompt 119 (resumo)

| Permissão | Uso |
|-----------|-----|
| `FINANCE_APPROVAL_REQUEST` | Abrir solicitação de aprovação |
| `FINANCE_APPROVAL_DECIDE` | Aprovar/rejeitar; editar política |
| `FINANCE_PAYMENT_APPROVE` | Alias operacional para decisão de pagamentos |
| `FINANCE_DISCOUNT_GRANT` | Conceder desconto em liquidações |
| `FINANCE_BALANCE_ACCESS` | Consulta explícita de saldo (auditada) |
| `BANK_ACCOUNT_SENSITIVE_READ` | Dados bancários mascarados |
| `FINANCE_CONSOLIDATED_READ` | Visão multiloja consolidada |
| `FINANCE_AUDIT_READ` | Consulta eventos financeiros |
| `FINANCE_MIGRATION_RUN` | Executar backfill |

Permissões anteriores (`V228`, `V233`, `V241`, `V245`, `V249`) permanecem válidas para CRUD e operações dos submódulos.

Roles `ADMIN` e `MANAGER` recebem seeds das permissões `V251`.
