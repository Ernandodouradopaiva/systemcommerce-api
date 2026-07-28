-- V214: Permissões Prompts 74–79
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000220', 'INVENTORY_COUNT_READ', 'Consultar inventários', 'INVENTORY',
     'Consultar contagens físicas/rotativas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000221', 'INVENTORY_COUNT_CREATE', 'Criar inventários', 'INVENTORY',
     'Planejar e abrir inventário', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000222', 'INVENTORY_COUNT_MANAGE', 'Operar inventários', 'INVENTORY',
     'Contar, analisar, aprovar', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000223', 'INVENTORY_COUNT_POST', 'Postar inventários', 'INVENTORY',
     'Postar ajustes de inventário', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000224', 'BATCH_READ', 'Consultar lotes', 'INVENTORY',
     'Consultar lotes e validade', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000225', 'BATCH_MANAGE', 'Gerenciar lotes', 'INVENTORY',
     'CRUD lotes e bloqueio', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000226', 'SERIAL_READ', 'Consultar séries', 'INVENTORY',
     'Consultar números de série', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000227', 'SERIAL_MANAGE', 'Gerenciar séries', 'INVENTORY',
     'CRUD e status de séries', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000228', 'BUNDLE_READ', 'Consultar kits', 'CATALOG',
     'Consultar kits/combos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000229', 'BUNDLE_MANAGE', 'Gerenciar kits', 'CATALOG',
     'CRUD kits e políticas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000230', 'BOM_READ', 'Consultar fichas técnicas', 'PRODUCTION',
     'Consultar BOM', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000231', 'BOM_MANAGE', 'Gerenciar fichas técnicas', 'PRODUCTION',
     'CRUD BOM versionada', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000232', 'PRODUCTION_READ', 'Consultar produção', 'PRODUCTION',
     'Consultar ordens de produção', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000233', 'PRODUCTION_MANAGE', 'Gerenciar produção', 'PRODUCTION',
     'Liberar e concluir produção', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'INVENTORY_COUNT_READ', 'INVENTORY_COUNT_CREATE', 'INVENTORY_COUNT_MANAGE', 'INVENTORY_COUNT_POST',
      'BATCH_READ', 'BATCH_MANAGE', 'SERIAL_READ', 'SERIAL_MANAGE',
      'BUNDLE_READ', 'BUNDLE_MANAGE', 'BOM_READ', 'BOM_MANAGE',
      'PRODUCTION_READ', 'PRODUCTION_MANAGE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
