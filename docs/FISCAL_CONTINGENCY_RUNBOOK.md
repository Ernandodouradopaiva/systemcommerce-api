# Runbook — Contingência Fiscal (Prompt 139)

> Operacionaliza `FiscalContingencyService` e endpoints `/api/v1/fiscal/contingencies`.  
> Complementa [FISCAL_CONTINGENCY.md](./FISCAL_CONTINGENCY.md).

---

## 1. Quando ativar

| Cenário | Ação |
|---------|------|
| Falha de **rede** na transmissão NFC-e | Auto-ativação soft (`OFFLINE_NFCE`) via `NfceEmissionService` se política permitir |
| SEFAZ indisponível (status serviço) | Ativação manual `FISCAL_CONTINGENCY_MANAGE` |
| Manutenção programada | Ativação manual com motivo documentado |

**Não ativar** por rejeição fiscal de negócio (cStat de validação/regra) — tratar como erro de documento.

---

## 2. Ativação manual

```http
POST /api/v1/fiscal/contingencies/activate
Authorization: Bearer {token}
Content-Type: application/json

{
  "establishmentId": "{uuid}",
  "model": "65",
  "environment": "HOMOLOGATION",
  "mode": "OFFLINE_NFCE",
  "reason": "SEFAZ indisponível — timeout 30s",
  "triggerKind": "MANUAL"
}
```

Permissão: `FISCAL_CONTINGENCY_MANAGE`

---

## 3. Emissão em contingência

1. Documento fiscal criado com `contingency=true` e status `CONTINGENCY_PENDING`.
2. Registrar vínculo: `POST /api/v1/fiscal/contingencies/{id}/documents/{documentId}`.
3. PDV continua operando; estoque/financeiro seguem regras comerciais.

---

## 4. Retransmissão pós-normalização

1. Consultar pendentes: `GET /api/v1/fiscal/contingencies/pending-documents`
2. Retransmitir lote: `POST /api/v1/fiscal/contingencies/{id}/retransmit-pending`
   - Consulta protocolo via `FiscalAuthorityAdapter.consultaProtocolo`
   - Registra tentativa em `fiscal_contingency_transmission_attempts`
3. Documento autorizado → status `AUTHORIZED`, `pending_retransmission=false`

---

## 5. Encerramento

```http
POST /api/v1/fiscal/contingencies/{id}/close
```

Verificar que não há documentos `PENDING` críticos ou documentar exceções operacionais.

---

## 6. Permissões

| Código | Uso |
|--------|-----|
| `FISCAL_CONTINGENCY_MANAGE` | Ativar, encerrar, registrar docs, retransmitir |
| `FISCAL_CONTINGENCY_READ` | Consultar ativa e pendentes |

---

## 7. Auditoria

Todas as ações registradas via `DomainAuditService` (`module=FISCAL`, entity `FiscalContingency`).

---

## 8. Escalation

- Falhas persistentes após 3 tentativas → acionar suporte fiscal / verificar certificado e conectividade.
- Divergência numeração → usar módulo de numeração (`FISCAL_NUMBERING_*`) antes de forçar retransmissão.
