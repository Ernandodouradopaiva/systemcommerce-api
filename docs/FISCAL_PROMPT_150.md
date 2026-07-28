# Prompt 150 — Migração, homologação e validação final

| Documento | Uso |
|-----------|-----|
| [FISCAL_MIGRATION_PLAN.md](./FISCAL_MIGRATION_PLAN.md) | Inventário e importação de histórico |
| [FISCAL_HOMOLOGATION_PLAN.md](./FISCAL_HOMOLOGATION_PLAN.md) | Matriz H01–H20 + fluxos integrados |
| [FISCAL_VALIDATION_REPORT.md](./FISCAL_VALIDATION_REPORT.md) | Relatório preenchível |
| [FISCAL_FINAL_CHECKLIST.md](./FISCAL_FINAL_CHECKLIST.md) | Checklist de conclusão |
| [FISCAL_PRODUCTION_READINESS.md](./FISCAL_PRODUCTION_READINESS.md) | Gate GO/NO-GO |
| [FISCAL_COMPLIANCE_REVIEW.md](./FISCAL_COMPLIANCE_REVIEW.md) | Parecer de conformidade |

## Código

- Status `AUTHORIZED_PENDING_INTEGRATION`
- `POST /api/v1/fiscal/migration/external-history` (`FISCAL_HISTORY_IMPORT`)
- Migration **V279**

**Regra:** não emitir DFe retroativo automaticamente; histórico externo preserva XML/protocolo/chave sem estoque/financeiro.
