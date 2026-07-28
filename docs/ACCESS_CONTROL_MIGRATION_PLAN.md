# Plano de migração do controle de acesso — SystemCommerce (Prompt 167)

## Estado atual

| Origem | Destino | Status |
|--------|---------|--------|
| `roles` | AccessGroup (mesmo físico) | Migrado conceitualmente (151+) |
| `role_permissions` | `group_permission_assignments` | Copiado em V281 |
| `user_roles` | `user_group_assignments` | Copiado em V281 |
| `permissions` | Catálogo + risk_level | V280 + V284 |
| `user_store_access` | Mantido | Pré-existente |
| Contingência | `ADMIN_CONTINGENCY` | Seed V284 |

## Etapas

1. **Antes** — executar `scripts/validate-access-control.sql` (contagens).
2. Aplicar Flyway **V280–V284** (não apagar tabelas legadas).
3. Validar: todo usuário ACTIVE tem ≥1 grupo efetivo **ou** está em lista de exceção.
4. Validar: não há permissão referenciada inexistente.
5. Validar: ≥1 usuário com grupo ADMIN/ADMIN_CONTINGENCY ativo.
6. Incrementar `users.access_version` globalmente após cutover (força re-login).
7. **Depois** — mesmo script de validação; comparar relatório.

## Rollback documentado

1. Não dropar `user_roles` / `role_permissions` nesta fase.
2. `PermissionResolver` mantém fallback legado se assignments vazios.
3. Para reverter V284: restaurar backup PostgreSQL pré-migração (Flyway repair não reverte DDL automaticamente).
4. Tokens antigos sem `sid` continuam válidos se `av` bater; sessões novas passam a exigir `sid` ativo quando presente.

## IDs preservados

- Roles/permissions seed UUIDs mantidos.
- Contingência: `a2000000-0000-4000-8000-000000000099`.

## Pós-validação (futuro)

Somente após checklist 169 completo: marcar joins legados como deprecated e planejar remoção em migration futura.
