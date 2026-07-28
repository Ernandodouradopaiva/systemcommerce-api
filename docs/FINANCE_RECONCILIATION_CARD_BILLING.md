# Conciliação bancária, cartões e cobranças (Prompts 111–113)

## Migrations

| Versão | Conteúdo |
|--------|----------|
| V242 | Extrato, importação, entries, regras, conciliação e matches |
| V243 | Adquirentes, bandeiras, planos de taxa, tx cartão, previsão, liquidação, chargeback |
| V244 | BillingDocument, BankSlip, PixCharge, histórico e webhooks |
| V245 | Permissões |

## 111 — Conciliação bancária

**Entidades:** `BankStatement`, `BankStatementEntry`, `BankStatementImport`, `BankReconciliation`, `BankReconciliationMatch`, `BankReconciliationRule`

**Status da entrada:** `UNMATCHED`, `SUGGESTED`, `MATCHED`, `PARTIALLY_MATCHED`, `IGNORED`, `DIVERGENT`

**Endpoints (`/api/v1`):**
- `GET/POST /bank-statements` (+ manual, entries)
- `POST /bank-statements/import/ofx` e `/import/csv`
- Regras, sugerir, confirmar, ignorar, desfazer, criar lançamento ausente

**Regras:**
- Hash SHA-256 evita importação duplicada do mesmo payload
- Sugestão de correspondência; auto-conciliação só com regras `SAFE` / confiança alta
- Confirmação manual; divergências visíveis; desfazer preserva extrato original
- Conciliação não gera nova movimentação se o movimento já existe
- Criar lançamento ausente exige `BANK_RECONCILIATION_CREATE_MISSING`
- Auditoria via `DomainAuditService`

## 112 — Cartões / adquirentes / previsão

**Entidades:** `Acquirer`, `CardBrand`, `CardFeePlan`, `CardTransaction`, `CardReceivableSchedule`, `CardSettlement`, `CardChargeback`

**Status:** `AUTHORIZED`, `CAPTURED`, `SCHEDULED`, `SETTLED`, `CANCELLED`, `CHARGEBACK`, `DIVERGENT`

**Endpoints:**
- CRUD adquirentes/bandeiras/planos
- `POST /card-transactions` — taxas calculadas na API; só `cardLastFour` (nunca PAN/CVV)
- `GET /card-receivable-schedules/forecast` — previsão de recebimento
- `POST /card-settlements` — liquidação + movimento no holder (conciliável com extrato)
- `POST /card-transactions/{id}/chargebacks`

**Regras:**
- Idempotência por chave; venda/pagamento não duplica
- Parcelas da adquirente podem diferir das do cliente (agenda própria)
- Chargeback marca ajuste / status `CHARGEBACK`
- Dados históricos preservados (sem exclusão física)

## 113 — Boletos e PIX

**Entidades:** `BillingDocument`, `BankSlip`, `PixCharge`, `BillingStatusHistory` (+ `BillingWebhookEvent`)

**Status:** `DRAFT`, `REGISTERED`, `PENDING`, `PAID`, `OVERDUE`, `CANCELLED`, `EXPIRED`, `REFUNDED`

**Adapter:** `BillingProviderAdapter` + `StubBillingProviderAdapter` (`STUB`). Domínio não acopla banco específico.

**Endpoints:**
- `POST/GET /billing-documents`, `POST .../register`, `POST .../cancel`
- `POST /billing/webhooks` — idempotente por `(organization, provider, eventId)`

**Regras:**
- Cobrança vinculada à parcela (AR); pagamento atualiza parcela
- PIX com `expiresAt`; boleto preserva linha digitável
- Cancelamento não apaga histórico
- Identificadores externos armazenados; integração real pode plugar novo adapter

## Permissões (front + seed V245)

`BANK_RECONCILIATION_*`, `CARD_ACQUIRER_*`, `CARD_SETTLEMENT_MANAGE`, `BILLING_*`
