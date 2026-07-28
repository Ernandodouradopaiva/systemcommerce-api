# PERMISSION_COVERAGE_REPORT.md (Prompt 169)

| Módulo | Permissões seed | Endpoint PreAuthorize | Front codes | Escopo |
|--------|-----------------|----------------------|-------------|--------|
| ACCESS | ACCESS_GROUP_*, ACCESS_CATALOG_*, PRIVILEGED_*, ACCESS_AUDIT_*, ACCESS_REVIEW_*, ACCESS_REPORT_*, SESSION_*, EFFECTIVE_*, HIERARCHY_* | Access* / Hierarchy / Governance | permissions.ts | ORG |
| USER | USER_* | UserController | sim | ORG |
| CUSTOMER | CUSTOMER_* | Customer* | sim | STORE |
| SALES | SALES_ORDER_*, QUOTE_*, SALE_* | Sales* | sim | STORE/OWN/TEAM |
| POS | POS_* | Pos* | sim | STORE |
| FINANCE | FINANCIAL_*, PAYABLE_* | Finance* | parcial | ORG/STORE |
| FISCAL | FISCAL_* | Fiscal* | parcial | ORG/STORE |
| INVENTORY | INVENTORY_* | Inventory* | sim | STORE |
| AUDIT | AUDIT_READ | AuditLogController | sim | ORG |

Atualizar este arquivo a cada nova funcionalidade (Prompt 170).
