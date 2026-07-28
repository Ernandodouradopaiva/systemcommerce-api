-- V151: permissões do módulo de profissionais / lotação
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000070', 'EMPLOYEE_READ', 'Consultar profissionais', 'EMPLOYEE',
     'Listar e consultar profissionais e lotações', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000071', 'EMPLOYEE_CREATE', 'Cadastrar profissional', 'EMPLOYEE',
     'Criar profissionais', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000072', 'EMPLOYEE_UPDATE', 'Atualizar profissional', 'EMPLOYEE',
     'Editar dados e vincular usuário', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000073', 'EMPLOYEE_ASSIGN_STORE', 'Gerenciar lotação', 'EMPLOYEE',
     'Criar, alterar e encerrar lotações por loja', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000074', 'EMPLOYEE_ASSIGNMENT_HISTORY', 'Histórico de lotação', 'EMPLOYEE',
     'Consultar histórico e lojas de atuação', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'EMPLOYEE_READ',
      'EMPLOYEE_CREATE',
      'EMPLOYEE_UPDATE',
      'EMPLOYEE_ASSIGN_STORE',
      'EMPLOYEE_ASSIGNMENT_HISTORY'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
