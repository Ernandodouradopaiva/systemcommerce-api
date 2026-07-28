-- V282: Permissões ACL AccessGroup (Prompts 153–155)
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version, system_permission, code_immutable)
VALUES
    ('a1000000-0000-4000-8000-000000000381', 'ACCESS_GROUP_READ', 'Consultar grupos', 'ACCESS',
     'Consultar grupos de usuários', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE),
    ('a1000000-0000-4000-8000-000000000382', 'ACCESS_GROUP_CREATE', 'Criar grupos', 'ACCESS',
     'Criar grupos de usuários', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE),
    ('a1000000-0000-4000-8000-000000000383', 'ACCESS_GROUP_UPDATE', 'Atualizar grupos', 'ACCESS',
     'Editar grupos de usuários', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE),
    ('a1000000-0000-4000-8000-000000000384', 'ACCESS_GROUP_DISABLE', 'Inativar grupos', 'ACCESS',
     'Ativar/inativar grupos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE),
    ('a1000000-0000-4000-8000-000000000385', 'ACCESS_GROUP_DUPLICATE', 'Duplicar grupos', 'ACCESS',
     'Duplicar grupo e permissões', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE),
    ('a1000000-0000-4000-8000-000000000386', 'ACCESS_GROUP_MEMBER_MANAGE', 'Gerenciar membros do grupo', 'ACCESS',
     'Vincular/desvincular usuários', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE),
    ('a1000000-0000-4000-8000-000000000387', 'ACCESS_GROUP_PERMISSION_MANAGE', 'Gerenciar permissões do grupo', 'ACCESS',
     'Conceder/remover permissões do grupo', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE),
    ('a1000000-0000-4000-8000-000000000388', 'ACCESS_CATALOG_READ', 'Consultar catálogo de acesso', 'ACCESS',
     'Consultar módulos, recursos e ações', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0, TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

UPDATE permissions p
SET module_id = m.id,
    resource_id = r.id
FROM system_modules m, system_resources r
WHERE p.code LIKE 'ACCESS_GROUP_%'
  AND m.code = 'ACCESS'
  AND r.code = 'ACCESS_GROUPS'
  AND r.module_id = m.id;

UPDATE permissions p
SET module_id = m.id,
    resource_id = r.id,
    action_id = a.id
FROM system_modules m, system_resources r, system_actions a
WHERE p.code = 'ACCESS_CATALOG_READ'
  AND m.code = 'ACCESS' AND r.code = 'PERMISSIONS' AND r.module_id = m.id AND a.code = 'READ';

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'ACCESS_GROUP_READ', 'ACCESS_GROUP_CREATE', 'ACCESS_GROUP_UPDATE', 'ACCESS_GROUP_DISABLE',
      'ACCESS_GROUP_DUPLICATE', 'ACCESS_GROUP_MEMBER_MANAGE', 'ACCESS_GROUP_PERMISSION_MANAGE',
      'ACCESS_CATALOG_READ'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO group_permission_assignments (
    id, group_id, permission_id, grant_type, scope, valid_from, status, active, created_at, updated_at, version)
SELECT gen_random_uuid(), r.id, p.id, 'ALLOW', 'ORGANIZATION', NOW() AT TIME ZONE 'UTC', 'ACTIVE', TRUE,
       NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'ACCESS_GROUP_READ', 'ACCESS_GROUP_CREATE', 'ACCESS_GROUP_UPDATE', 'ACCESS_GROUP_DISABLE',
      'ACCESS_GROUP_DUPLICATE', 'ACCESS_GROUP_MEMBER_MANAGE', 'ACCESS_GROUP_PERMISSION_MANAGE',
      'ACCESS_CATALOG_READ'
  )
ON CONFLICT (group_id, permission_id) DO NOTHING;
