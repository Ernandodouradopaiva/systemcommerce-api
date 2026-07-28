# ACCESS_CONTROL_VALIDATION_REPORT.md (Prompt 169)

Data: 2026-07-23

## Escopo validado

Módulos: Administração, Cadastros, Produtos, Compras, Estoque, Vendas, PDV, Financeiro, Fiscal, Relatórios, Auditoria, Integrações.

## Evidências

| Controle | Evidência |
|----------|-----------|
| RBAC grupos | `Role` = AccessGroup; assignments V281+ |
| Escopos | `PermissionScopeType` + resolvers |
| Efetivas | `EffectivePermissionService` + cache por `av` |
| Auth central | `@authorizationService.*` |
| Sensíveis | `risk_level` + `PrivilegedAccessService` |
| Sessão | `user_sessions` + claim `sid` |
| Menu API | `GET /api/v1/me/menus` |
| Auditoria | `access_audit_events` + tela `/administracao/auditoria-acesso` |
| Revisão | `access_reviews` |
| Migração | `ACCESS_CONTROL_MIGRATION_PLAN.md` + script SQL |

## Lacunas conhecidas (dívida)

- Nem todos os controllers legados usam `@authorizationService` (checklist parcial reporta).
- Cobertura OWN/TEAM limitada a `SALES_ORDER` no `OwnershipResolver`.
- Campos avançados de usuário (telefone/convite) ainda no roadmap do cadastro.

## Critério “não concluído”

Bloqueadores listados no Prompt 169 devem ser tratados continuamente; este relatório registra a base profissional entregue em 151–170.
