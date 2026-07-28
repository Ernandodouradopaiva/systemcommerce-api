-- V101: perfil administrador com todas as permissões
-- Usuário admin é criado via seed Java (BCrypt a partir de ADMIN_PASSWORD)

INSERT INTO roles (id, code, name, description, active, created_at, updated_at, version)
VALUES (
    'b1000000-0000-4000-8000-000000000001',
    'ADMIN',
    'Administrador',
    'Acesso total ao SystemCommerce',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
);

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT
    'b1000000-0000-4000-8000-000000000001'::uuid,
    p.id,
    NOW() AT TIME ZONE 'UTC'
FROM permissions p
WHERE p.active = TRUE;
