# Evolução ERP comercial — visão unificada (Prompt 56)

> Distingue esta série (**ERP profissional 56–90**) da numeração multiloja legada (também “Prompt 56…” em `MULTISTORE_*`).

## Documentos desta revisão

| Arquivo | Conteúdo |
|---|---|
| [PURCHASE_ARCHITECTURE.md](./PURCHASE_ARCHITECTURE.md) | Cadeia de compras, estados, numeração, permissões |
| [SALES_ARCHITECTURE.md](./SALES_ARCHITECTURE.md) | Cadeia de vendas, faturamento, reserva |
| [INVENTORY_ARCHITECTURE.md](./INVENTORY_ARCHITECTURE.md) | Movimentações, saldos, estoque como consequência |
| [DOCUMENT_TRACEABILITY.md](./DOCUMENT_TRACEABILITY.md) | Origem tipada, conversões, snapshots |
| [FISCAL_ARCHITECTURE.md](./FISCAL_ARCHITECTURE.md) | DFe (NF-e/NFC-e) integrado sem duplicar comercial (Prompt 121+) |

## Cadeias

**Compras:** Solicitação → Cotação fornecedor → Pedido (`PurchaseOrder`) → Recebimento (`PurchaseReceipt`) → Estoque → Conta a pagar → entrada fiscal (XML/vínculo).

**Vendas:** Orçamento (`Quote`) → Pedido (`SalesOrder`) → Reserva → Separação → Expedição → Faturamento → `Sale` + estoque → Conta a receber → NF-e (55).

**PDV:** `Sale` (POS) + pagamentos + estoque + finance → NFC-e (65) opcional/configurável.

## Ausência de duplicação

| Não criar | Usar |
|---|---|
| `SalesQuotation` tabela nova | `quotes` / `Quote` |
| `goods_receipts` | `purchase_receipts` / `PurchaseReceipt` |
| `Salesperson` | `SellerProfile` |
| Segundo saldo de estoque | `inventory` + `stock_movements` |

## Critérios de aceite Prompt 56

| Critério | Status |
|---|---|
| Arquitetura documentada | OK |
| Compras e vendas integradas (modelo) | OK |
| Estoque como consequência | OK |
| Rastreabilidade definida | OK |
| Multiloja | OK |
| Sem entidades duplicadas | OK |
| Estratégia de migração | OK |
