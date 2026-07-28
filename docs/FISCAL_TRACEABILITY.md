# Rastreabilidade e trilha fiscal — SystemCommerce

> Complementa [FISCAL_ARCHITECTURE.md](./FISCAL_ARCHITECTURE.md) e [DOCUMENT_TRACEABILITY.md](./DOCUMENT_TRACEABILITY.md).  
> Pacotes: `document`, `storage`, `event`, `monitoring`, `security`.

---

## 1. Objetivos

1. Saber, a qualquer momento, o **status** de cada DFe e como chegou lá.
2. Preservar **XML enviado e recebido**, protocolos e eventos.
3. Ligar DFe ↔ documento comercial (`Sale`, `SalesOrder`, `PurchaseReceipt`, …).
4. Impedir exclusão de nota autorizada.
5. Permitir monitor operacional e auditoria forense.

---

## 2. Extensão do enum de origem

Incluir em `OriginDocumentType` (e/ou espelho fiscal):

```
SALE, SALES_ORDER, SALES_ORDER_BILLING, PURCHASE_RECEIPT,
FISCAL_DOCUMENT, FISCAL_EVENT, FISCAL_INUTILIZATION
```

Conversão tipada:

| From | To |
|---|---|
| SALE / SALES_ORDER_BILLING | FISCAL_DOCUMENT (55/65) |
| FISCAL_DOCUMENT | FISCAL_EVENT (cancel, CCe, …) |
| PURCHASE_RECEIPT | FISCAL_DOCUMENT (entrada / vínculo) |
| FISCAL_DOCUMENT | FISCAL_DOCUMENT (devolução/complementar via reference) |

Manter FKs diretas (`sale_id`, `sales_order_id`, …) para join rápido + `document_conversions` quando o hub existir.

---

## 3. Histórico de status

`FiscalDocumentStatusHistory` (append-only):

| Campo | Uso |
|---|---|
| `from_status` / `to_status` | Transição |
| `at` / `by_user_id` | Quando / quem |
| `correlation_id` | Rastreio HTTP |
| `sefaz_cstat` / `xmotivo` | Snapshot retorno |
| `details` | JSON sanitizado |

Espelha o espírito de `SalesOrderBillingHistory`.

---

## 4. Storage de XML e protocolos

`FiscalXmlStorage`:

- `kind`: `OUTBOUND_SIGNED`, `INBOUND_RETORNO`, `AUTHORIZED`, `EVENT_OUT`, `EVENT_RET`, `CANCELLED`, …
- `content` (bytea ou object storage) + `sha256`
- `layout_version_id`
- `stored_at` — imutável

`FiscalProtocol`:

- `type`: `RECEIPT`, `AUTHORIZATION`, `EVENT`, …
- `number`, `received_at`, `raw_payload_ref`

Retenção: política configurável; **não** apagar XML de documentos autorizados em operação normal (apenas arquivamento cold se necessário, mantendo hash/metadados).

---

## 5. Eventos e correções

```
FiscalDocument (AUTHORIZED)
    └── FiscalEvent (CANCELAMENTO | CCE | MANIFESTACAO | …)
            └── Xml + Protocol + StatusHistory
```

- CCe não altera XML autorizado original.
- Cancelamento marca documento `CANCELLED` sem delete.
- Inutilização é entidade própria ligada à série/numeração.

---

## 6. Monitor fiscal

Visões:

- fila `PROCESSING` / rejeitados / contingência pendente;
- certificados a vencer;
- divergência de totais comercial × fiscal;
- documentos sem protocolo após SLA.

API sugerida: `/api/v1/fiscal/monitor/...` (somente backend agrega).

---

## 7. Idempotência e concorrência

- `idempotency_key` única por emissão.
- Numeração: lock pessimista/otimista na série.
- Reconsulta SEFAZ reconciliável com status local (não cria segundo documento ativo).

---

## 8. Critérios de aceite

- [x] Histórico de status + XML + protocolos definidos
- [x] Vínculo comercial tipado
- [x] Nota autorizada não apagável
- [x] Eventos como correção
- [x] Monitor e auditoria previstos
