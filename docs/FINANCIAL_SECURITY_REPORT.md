# Relatório de segurança financeira (Prompt 119)

> Controles de permissão, auditoria e aprovação em duas etapas do módulo financeiro.  
> Migrations: `V250` (políticas, solicitações, `finance_migration_runs`), `V251` (permissões).

---

## 1. Permissões

### Novas — `V251`

| Código | Descrição |
|--------|-----------|
| `FINANCE_DISCOUNT_GRANT` | Conceder desconto em liquidações/parcelas |
| `FINANCE_PAYMENT_APPROVE` | Aprovar operações financeiras sensíveis |
| `BANK_ACCOUNT_SENSITIVE_READ` | Visualizar dados bancários mascarados (não segredos) |
| `FINANCE_CONSOLIDATED_READ` | Visão consolidada multiloja |
| `FINANCE_APPROVAL_REQUEST` | Abrir solicitação de aprovação |
| `FINANCE_APPROVAL_DECIDE` | Aprovar/rejeitar solicitações; alterar política |
| `FINANCE_MIGRATION_RUN` | Executar backfill financeiro |
| `FINANCE_AUDIT_READ` | Consultar auditoria financeira |
| `FINANCE_BALANCE_ACCESS` | Acesso explícito a consulta de saldo |

Seeds: roles `ADMIN` e `MANAGER`.

### Existentes (referência)

Permissões dos prompts anteriores permanecem necessárias para operação:

- `V228` — cadastros mestres (`FINANCIAL_ACCOUNT_*`, `BANK_ACCOUNT_*`, …)
- `V233` — AP/AR (`PAYABLE_*`, `RECEIVABLE_*`)
- `V241` — operações (`FINANCIAL_ENTRY_*`, `FINANCIAL_TRANSFER_*`, …)
- `V245` — conciliação/cartões/cobrança
- `V249` — analytics (`CASH_FLOW_*`, `FINANCIAL_PERIOD_*`, …)

Consulta de saldo aceita `BANK_ACCOUNT_BALANCE_READ` **ou** `FINANCE_BALANCE_ACCESS` (`BankFinanceController`).

---

## 2. Auditoria financeira

### `FinanceAuditEvents`

Códigos padronizados registrados via `FinanceAuditService` → `DomainAuditService`:

`FINANCE_CREATE`, `FINANCE_UPDATE`, `FINANCE_PAYMENT`, `FINANCE_RECEIPT`, `FINANCE_REVERSAL`, `FINANCE_DISCOUNT`, `FINANCE_INTEREST`, `FINANCE_FINE`, `FINANCE_TRANSFER`, `FINANCE_RECONCILIATION`, `FINANCE_PERIOD_CLOSE`, `FINANCE_PERIOD_REOPEN`, `FINANCE_IMPORT`, `FINANCE_EXPORT`, `FINANCE_BALANCE_ACCESS`, `FINANCE_DENIED_ACCESS`, `FINANCE_APPROVAL_REQUEST`, `FINANCE_APPROVAL_DECIDE`.

### `AuditSanitizer`

Snapshots de auditoria (`oldValues` / `newValues`) passam por `AuditSanitizer.sanitize()` antes de persistir.

**Campos redigidos (`[REDACTED]`):**

- Senhas e hashes (`password`, `passwordHash`, …)
- Tokens (`token`, `accessToken`, `refreshToken`, `authorization`, …)
- Cartão (`cardNumber`, `cvv`, `pan`, `fullPan`, …)
- Segredos bancários (`bankSecret`, `bankPassword`, `pixSecret`, …)
- Chaves/certificados (`privateKey`, `clientCertificate`, …)

Cobertura testada: `AuditSanitizerFinanceTest`, `AuditSanitizerTest`.

### Consulta de saldo auditada

Toda chamada a `BankFinanceService.balance()` registra evento `FINANCE_BALANCE_ACCESS` com entidade `FinancialAccountHolder`.

Acesso negado registra `FINANCE_DENIED_ACCESS` via `FinanceAuditService.denied()`.

---

## 3. Aprovação em duas etapas

### Entidades (`V250`)

- `financial_approval_policies` — uma política por organização (defaults seguros na primeira leitura)
- `financial_approval_requests` — solicitações com status `PENDING` → `APPROVED` / `REJECTED` → `EXECUTED`

### Tipos de operação (`FinancialApprovalRequest.OperationType`)

| Tipo | Política (`FinancialApprovalPolicy`) | Default |
|------|--------------------------------------|---------|
| `HIGH_PAYMENT` | `require_payment_approval` + `payment_approval_threshold` | desligado |
| `REVERSAL` | `require_reversal_approval` | **ligado** |
| `DISCOUNT` | `require_discount_approval` + `discount_approval_threshold` | **ligado** |
| `TRANSFER` | `require_transfer_approval` + `transfer_approval_threshold` | desligado |
| `PERIOD_REOPEN` | `require_period_reopen_approval` | **ligado** |
| `MANUAL_ENTRY` | `require_manual_entry_approval` + `manual_entry_approval_threshold` | desligado |

### Regra solicitante ≠ aprovador

Em `FinancialApprovalService.decide()`:

> Se `CurrentUser.id()` == `approval.requestedBy` → `BusinessRuleException`: *"Aprovação em duas etapas: o solicitante não pode decidir a própria solicitação"*.

Fluxo:

1. Solicitante com `FINANCE_APPROVAL_REQUEST` cria solicitação (`PENDING`)
2. Aprovador distinto com `FINANCE_APPROVAL_DECIDE` decide (`APPROVE` / `REJECT`)
3. Serviço de domínio valida `assertApprovedOrNotRequired()` antes de executar
4. Após execução, `markExecuted()` atualiza status para `EXECUTED`

### Integrações que consultam aprovação

- `FinancialTransferService` — transferências acima do limite
- `FinancialClosingService` — reabertura de período
- Liquidações/descontos/estornos conforme política da organização

---

## 4. Endpoints de aprovação

| Método | Rota | Permissão |
|--------|------|-----------|
| GET | `/api/v1/financial-approval-policies/{organizationId}` | `FINANCE_APPROVAL_REQUEST` ou `FINANCE_APPROVAL_DECIDE` |
| PUT | `/api/v1/financial-approval-policies/{organizationId}` | `FINANCE_APPROVAL_DECIDE` |
| GET | `/api/v1/financial-approval-requests?organizationId=` | `FINANCE_APPROVAL_REQUEST` ou `FINANCE_APPROVAL_DECIDE` |
| POST | `/api/v1/financial-approval-requests` | `FINANCE_APPROVAL_REQUEST` |
| POST | `/api/v1/financial-approval-requests/{id}/decide` | `FINANCE_APPROVAL_DECIDE` |

---

## 5. Operações que exigem aprovação (quando política ativa)

| Operação | Tipo | Observação |
|----------|------|------------|
| Pagamento de alto valor | `HIGH_PAYMENT` | Valor ≥ limiar configurado |
| Estorno / reversão | `REVERSAL` | Default exige aprovação |
| Desconto em liquidação | `DISCOUNT` | Valor ≥ limiar |
| Transferência entre holders | `TRANSFER` | Valor ≥ limiar |
| Reabertura de período fechado | `PERIOD_REOPEN` | Default exige aprovação |
| Lançamento manual | `MANUAL_ENTRY` | Valor ≥ limiar |

Sem `approvalRequestId` aprovado, a API retorna erro de regra de negócio — operação **não** é executada silenciosamente.

---

## 6. Referências

- Checklist: [`FINANCIAL_FINAL_CHECKLIST.md`](./FINANCIAL_FINAL_CHECKLIST.md)
- Validação: [`FINANCIAL_VALIDATION_REPORT.md`](./FINANCIAL_VALIDATION_REPORT.md)
- Código: `finance/approval/*`, `finance/security/*`, `shared/audit/AuditSanitizer.java`
