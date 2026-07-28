# Plano de migração financeira (Prompt 120)

> Backfill idempotente de dados comerciais legados para AR/AP sem duplicar documentos financeiros já existentes.  
> Migrations: `V229`–`V234` (origens e integração), `V250` (`finance_migration_runs`).

---

## Objetivo

Converter, por organização, o histórico operacional já presente no ERP em títulos financeiros consistentes:

| Origem legada | Destino financeiro | Critério de seleção |
|---------------|-------------------|---------------------|
| Vendas (`sales`) com pagamento confirmado/parcial | Conta a receber (`receivables`) | `status IN ('CONFIRMED','PAID','PARTIALLY_PAID')`, `customer_id` preenchido, `total_amount > 0` |
| Recebimentos de compra postados | Conta a pagar (`payables`) | `purchase_receipts.status IN ('POSTED_TO_INVENTORY','ACCEPTED','CONFIRMED')` |
| Movimentos de caixa PDV (`cash_movements`) | Já vinculados via `financial_holder_movement_id` (V234) | **Não recriar** — apenas validar vínculo |
| Contas bancárias / holders / caixas | Tabelas da era `V226` | **Não recriar** — cadastros já existentes |
| Fornecedores / clientes | Entidades comerciais existentes | Preservados nos documentos de origem |

### O que deve ser preservado

- IDs dos documentos comerciais (venda, recebimento)
- Valores, datas de emissão/vencimento e status financeiro derivado
- Vínculos loja, cliente, fornecedor, vendedor
- Trilha de auditoria (`FinanceAuditService`, `finance_migration_runs`)

### Idempotência

A migração **não duplica** títulos graças às tabelas de origem:

- `receivable_origins` — chave lógica `(origin_type, origin_document_id)` com tipos `SALE` / `POS`
- `payable_origins` — chave lógica `(origin_type, origin_document_id)` com tipo `PURCHASE_RECEIPT`

Registros já com origem registrada são **ignorados** na consulta de backfill.

---

## Endpoint

```
POST /api/v1/finance/migration/backfill?organizationId={uuid}&dryRun={true|false}
```

| Parâmetro | Descrição |
|-----------|-----------|
| `organizationId` | Organização alvo (obrigatório) |
| `dryRun` | Padrão `true`. Simula contagem e notas sem persistir AR/AP |

**Permissão:** `FINANCE_MIGRATION_RUN` (seed `V251`).

**Resposta (`MigrationResult`):** `runId`, `dryRun`, `status`, contadores (`salesScanned`, `receivablesCreated`, `purchasesScanned`, `payablesCreated`, `skippedDuplicates`, `errorsCount`) e `notes`.

Implementação: `FinanceBackfillService` → delega a `ReceivableService.generateFromSale` e `PayableService.generateFromPurchaseReceipt`.

---

## Procedimento recomendado

### 1. Dry-run (`dryRun=true`)

```http
POST /api/v1/finance/migration/backfill?organizationId={orgId}&dryRun=true
```

- Registra execução em `finance_migration_runs` com `dry_run=true`
- Lista vendas/recebimentos elegíveis (lote de até 500 por tipo)
- Retorna notas `DRY-RUN AR sale=…` / `DRY-RUN AP receipt=…`
- **Não** persiste receivables/payables

### 2. Revisão

Conferir:

- Contagens coerentes com expectativa operacional
- Ausência de erros em `notes` / `errorsCount`
- Saldos de holders (`V226`) já corretos — migração **não** recria bancos/caixas
- Vendas PDV já integradas via `PosFinanceIntegrationService` — devem aparecer como duplicatas evitadas se origem `POS` existir

### 3. Execução (`dryRun=false`)

```http
POST /api/v1/finance/migration/backfill?organizationId={orgId}&dryRun=false
```

- Cria AR/AP usando serviços oficiais de integração
- Idempotency keys: `migration-sale-{saleId}`, `migration-receipt-{receiptId}`
- Auditoria: evento `FINANCE_IMPORT` (`FinanceAuditEvents.IMPORT`)

### 4. Reconciliação de saldos

Após execução:

1. Comparar saldo de holders (`GET /api/v1/bank-accounts/{id}/balance`) com movimentos esperados
2. Validar agendas AP/AR (`/payables/agenda`, `/receivables/agenda`)
3. Conferir que `cash_movements.financial_holder_movement_id` permanece consistente (V234)
4. Registrar discrepâncias — **não** reexecutar backfill cegamente; investigar origem comercial

---

## Mapa de conversão

| Documento comercial | Status elegível | Título financeiro | Origem |
|--------------------|-----------------|-------------------|--------|
| Venda ERP / PDV | `CONFIRMED`, `PAID`, `PARTIALLY_PAID` | Receivable + parcelas | `SALE` ou `POS` |
| Recebimento de compra | `POSTED_TO_INVENTORY` (+ `ACCEPTED`, `CONFIRMED` no backfill) | Payable + parcelas | `PURCHASE_RECEIPT` |
| Pedido de compra (sem recebimento) | — | Manual ou `ON_ORDER_APPROVED` (config) | Fora do escopo do backfill padrão |
| Conta bancária / caixa financeiro | Já em `V226` | Holder + movimentos | Não migrar |
| Sangria/suprimento PDV | `CashMovement` | `FinancialHolderMovement` | Vínculo `financial_holder_movement_id` |

---

## Riscos

| Risco | Mitigação |
|-------|-----------|
| Duplicata de AR/AP | Filtro `NOT EXISTS` em `receivable_origins` / `payable_origins`; serviços idempotentes |
| Valor divergente pós-migração | Reconciliar holders e agendas antes de declarar concluído |
| Status de recebimento heterogêneo | Backfill tenta `POSTED_TO_INVENTORY`, `ACCEPTED`, `CONFIRMED`; erros registrados em `notes` |
| Lote limitado (500) | Reexecutar até `salesScanned + purchasesScanned = 0` elegíveis |
| Período financeiro fechado | Liquidações retroativas podem exigir reabertura aprovada (`FinancialApprovalPolicy`) |

---

## Rollback (soft)

Não há exclusão física automatizada de títulos migrados.

1. **Não apagar** receivables/payables criados — histórico financeiro deve permanecer
2. Marcar a execução problemática: status `FAILED` em `finance_migration_runs` (automático quando `errorsCount > 0` e nenhum título criado)
3. Corrigir causa raiz (dados comerciais, configuração, permissões)
4. Títulos criados indevidamente: cancelar via fluxos oficiais (`/payables/{id}/cancel`, cancelamento de AR aberta) — **não** DELETE direto
5. Reexecutar backfill apenas para registros ainda sem origem

---

## Referências

- Integração compras: [`FINANCE_PURCHASE_INTEGRATION.md`](./FINANCE_PURCHASE_INTEGRATION.md)
- Integração vendas: [`FINANCE_SALES_INTEGRATION.md`](./FINANCE_SALES_INTEGRATION.md)
- Integração PDV: [`PDV_FINANCIAL_INTEGRATION.md`](./PDV_FINANCIAL_INTEGRATION.md)
- AR/AP: [`FINANCE_AP_AR.md`](./FINANCE_AP_AR.md)
- Serviço: `finance/migration/service/FinanceBackfillService.java`
