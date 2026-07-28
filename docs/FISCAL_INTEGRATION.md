# Integração fiscal × domínios existentes — SystemCommerce

> Fronteira definitiva entre **fiscal**, **comercial**, **estoque** e **financeiro**.  
> Base: [FISCAL_ARCHITECTURE.md](./FISCAL_ARCHITECTURE.md).  
> Espelha a regra de [`FINANCIAL_INTEGRATION_REPORT.md`](./FINANCIAL_INTEGRATION_REPORT.md).

---

## 1. Regra de integração

```
Estoque     = quantidades   (já definidas pelo comercial/logística)
Financeiro  = valores       (títulos/liquidações a partir do comercial)
Fiscal      = obrigação DFe (XML + SEFAZ) a partir do comercial
Vínculo     = organization_id + store_id + origin_document_type/id
```

| Módulo | Pode escrever | Não pode |
|---|---|---|
| Comercial / PDV | Sale, Payment, estoque, AR (via finance integration) | XML SEFAZ |
| Financeiro | Payable/Receivable/holders | Estoque, XML fiscal |
| Fiscal | FiscalDocument, XML, eventos, protocolos | Sale/Payment, stock_movements, títulos |

Anti-duplicidade fiscal: `origin_document_type` + `origin_document_id` + `model` + `purpose` + `idempotency_key` únicos para documento “ativo”.

---

## 2. Mapa de integrações

| Domínio | Papel na emissão / entrada |
|---|---|
| Organização | Tenant; políticas default |
| Lojas | Contexto; flags `allowsSales` / `allowsPos` |
| Estabelecimento fiscal | CNPJ, IE, UF, CRT, CSC, série |
| Clientes | Destinatário / consumidor |
| Fornecedores | Emitente da NF de entrada; participante |
| Produtos + tabelas de preço | Itens; preço comercial → base de cálculo (regra backend) |
| Orçamentos | Sem emissão (só origem eventual) |
| Pedidos de venda | Faturamento parcial; referência |
| Separação / expedição | Dados de volumes/transportadora para NF-e |
| Faturamento comercial | Gatilho ERP para NF-e |
| Vendas (`Sale`) | Âncora da emissão 55/65 |
| PDV | Gatilho NFC-e |
| Compras / recebimento | Entrada fiscal (XML fornecedor + vínculo) |
| Estoque | Somente leitura de consequência já ocorrida |
| Pagamentos | Conferência de totais; não geram NF |
| Contas a pagar / receber | Independentes; fiscal não regenera |
| Transportadoras | `Carrier` no transporte da NF-e |
| Auditoria | `FiscalAudit` + `DomainAuditService` |
| Relatórios | Cubos fiscais + joins comerciais |

---

## 3. Vendas ERP

```
Quote → SalesOrder → … → invoice() → Sale (+ estoque + AR)
                                    ↓
                         FiscalIntegration.emitNfe(sale / billingHistory)
```

- Emissão **opcional/configurável** (`FiscalGenerationSettings` por org/loja): `DISABLED | MANUAL | ON_INVOICE | ON_DEMAND`.
- Faturamento parcial: cada `SalesOrderBillingHistory` pode gerar NF própria ou compor itens; sempre com referência ao pedido.
- Cancelamento comercial ≠ cancelamento fiscal: políticas coordenadas (ex.: exigir cancelamento de NF antes de estornar estoque, quando já autorizada).

---

## 4. PDV

```
Checkout → Sale POS → estoque → PosFinanceIntegration
                    → FiscalIntegration.emitNfce (se habilitado)
```

- Recibo não fiscal permanece disponível como fallback operacional.
- Rejeição SEFAZ: venda comercial válida; documento fiscal em `REJECTED` / fila de reprocessamento.

---

## 5. Compras / entrada fiscal

```
PurchaseOrder → PurchaseReceipt (post estoque → AP)
                      ↓
         FiscalInboundService.attachSupplierXml / emitir próprio (devolução)
```

- `PurchaseReceipt` já possui número/série/data de referência — evoluir para chave de acesso + XML armazenado.
- Entrada **não** remova/recria movimento de estoque; apenas enriquece o vínculo fiscal.
- Manifestação do destinatário opera sobre DF-e de terceiros vinculados à org/loja.

---

## 6. Devolução, complementar, ajuste

| Tipo | Estoque | Financeiro | Fiscal |
|---|---|---|---|
| Devolução | Movimento comercial já existente (`CUSTOMER_RETURN` etc.) | Estorno/título conforme finance | NF referenciando chave original |
| Complementar | Em geral nenhum | Ajuste de valor se houver processo | NF complementar |
| Ajuste | Conforme processo legal/comercial | Conforme processo | NF de ajuste quando aplicável |

Referências: `FiscalDocumentReference` + [FISCAL_TRACEABILITY.md](./FISCAL_TRACEABILITY.md).

---

## 7. Settings (`FiscalGenerationSettings`)

Campos conceituais (por organização, override por loja):

- `nfe_mode`, `nfce_mode` (`DISABLED|MANUAL|AUTO`)
- `environment_default` (`HOMOLOGATION|PRODUCTION`)
- `block_commercial_cancel_if_authorized`
- `amount_tolerance` (BigDecimal)
- `require_approval_to_cancel` / `require_approval_to_inutilize`
- `contingency_policy`

Espelha `FinanceGenerationSettings` (V234+).

---

## 8. Pontos de extensão no código atual

| Classe / fluxo atual | Extensão fiscal |
|---|---|
| `SalesOrderService.invoice` | Hook pós-faturamento |
| `PosCheckoutService` (finalize) | Hook pós-checkout |
| `pos.receipt` | Opcional: anexar chave/QR se NFC-e autorizada |
| `PurchaseReceipt` post | Hook entrada / validação XML |
| `pos.cancellation` / cancel `Sale` | Orquestração com `fiscal.event` |
| `DomainAuditService` | Eventos `FISCAL_*` |

Pacote: `fiscal.integration` (somente orquestração; sem regra tributária duplicada).

---

## 10. Estados intermediários pós-autorização (Prompt 150)

Quando a SEFAZ autoriza o DFe, operações internas (estoque/financeiro) podem ainda falhar.
Orquestradores devem usar:

```
… → AUTHORIZED_PENDING_INTEGRATION → (estoque + financeiro OK) → AUTHORIZED
```

Se a integração interna falhar, o documento permanece em `AUTHORIZED_PENDING_INTEGRATION` com
monitoramento/dead-letter — **nunca** reverter a autorização fiscal nem duplicar movimento.

API auxiliar: `/api/v1/fiscal/migration/documents/{id}/pending-integration` e `…/complete-integration`.

Histórico externo (`external_import=true`) **não** passa por essa fila (já não gera estoque/financeiro).

Ver: [FISCAL_MIGRATION_PLAN.md](./FISCAL_MIGRATION_PLAN.md), [FISCAL_FINAL_CHECKLIST.md](./FISCAL_FINAL_CHECKLIST.md).
