# Prompts 143–149 — Distribuição, monitor, reforma, storage, auditoria, dashboard e testes

| Prompt | Entrega | Migration / Doc |
|--------|---------|------------------|
| 143 | Distribuição DFe + manifestação | V272 |
| 144 | Monitor + fila / dead-letter | V273 |
| 145 | Reforma / layout versions | V274 + FISCAL_VERSIONING |
| 146 | Storage XML | V275 + FISCAL_STORAGE / BACKUP_RESTORE |
| 147 | Auditoria fiscal | V276 + FISCAL_AUDIT |
| 148 | Dashboard / relatórios | API `/fiscal/dashboard` |
| 149 | Suíte de testes | `*ServiceTest` + verify |
| Perms | | V277 |

## APIs

- `/api/v1/fiscal/dfe-distribution`
- `/api/v1/fiscal/monitor`
- `/api/v1/fiscal/layout-versions`
- `/api/v1/fiscal/storage`
- `/api/v1/fiscal/audit-events`
- `/api/v1/fiscal/dashboard` e `/api/v1/fiscal/reports/{type}`
