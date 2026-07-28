-- V110: permissão AUDIT_READ (consulta de auditoria)

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES (
    'a1000000-0000-4000-8000-00000000001b',
    'AUDIT_READ',
    'Consultar auditoria',
    'AUDIT',
    'Consulta paginada da trilha de auditoria',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code = 'AUDIT_READ'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
