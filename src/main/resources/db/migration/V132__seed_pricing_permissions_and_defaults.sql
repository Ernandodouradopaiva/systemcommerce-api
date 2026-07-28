-- V132: permissões + seeds de preço/desconto

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)

VALUES

('a1000000-0000-4000-8000-000000000031', 'PRICE_TABLE_READ', 'Consultar tabelas de preço', 'PRICING',

 'Consultar tabelas e preço aplicável', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),

('a1000000-0000-4000-8000-000000000032', 'PRICE_TABLE_MANAGE', 'Gerir tabelas de preço', 'PRICING',

 'Cadastrar/editar tabelas, vincular produtos e lojas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),

('a1000000-0000-4000-8000-000000000033', 'DISCOUNT_POLICY_READ', 'Consultar políticas de desconto', 'PRICING',

 'Consultar políticas e limites do operador', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),

('a1000000-0000-4000-8000-000000000034', 'DISCOUNT_POLICY_MANAGE', 'Gerir políticas de desconto', 'PRICING',

 'Cadastrar/editar políticas e limites por perfil', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),

('a1000000-0000-4000-8000-000000000035', 'POS_DISCOUNT_AUTHORIZE', 'Autorizar desconto elevado no PDV', 'POS',

 'Aprovar ou negar solicitação de desconto acima do limite', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)

ON CONFLICT (code) DO NOTHING;



INSERT INTO role_permissions (role_id, permission_id, created_at)

SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'

FROM roles r CROSS JOIN permissions p

WHERE r.code = 'ADMIN'

  AND p.code IN ('PRICE_TABLE_READ','PRICE_TABLE_MANAGE','DISCOUNT_POLICY_READ','DISCOUNT_POLICY_MANAGE','POS_DISCOUNT_AUTHORIZE')

  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);



INSERT INTO role_permissions (role_id, permission_id, created_at)

SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'

FROM roles r CROSS JOIN permissions p

WHERE r.code = 'MANAGER'

  AND p.code IN ('PRICE_TABLE_READ','PRICE_TABLE_MANAGE','DISCOUNT_POLICY_READ','DISCOUNT_POLICY_MANAGE','POS_DISCOUNT_AUTHORIZE')

  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);



INSERT INTO role_permissions (role_id, permission_id, created_at)

SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'

FROM roles r CROSS JOIN permissions p

WHERE r.code = 'SELLER'

  AND p.code IN ('PRICE_TABLE_READ','DISCOUNT_POLICY_READ')

  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);



-- Tabela padrão (fallback global)

INSERT INTO price_tables (id, code, name, description, status, priority, active, created_at, updated_at, version)

VALUES (

    'd1000000-0000-4000-8000-000000000001',

    'PADRAO',

    'Tabela padrão',

    'Preços padrão sincronizados a partir do cadastro de produtos',

    'ACTIVE',

    0,

    TRUE,

    NOW() AT TIME ZONE 'UTC',

    NOW() AT TIME ZONE 'UTC',

    0

)

ON CONFLICT (code) DO NOTHING;



-- Limites por perfil

INSERT INTO operator_discount_limits (id, role_id, max_percent, max_amount, active, created_at, updated_at, version)

SELECT 'd2000000-0000-4000-8000-000000000001', r.id, 10.0000, NULL, TRUE,

       NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0

FROM roles r WHERE r.code = 'SELLER'

ON CONFLICT (role_id) DO NOTHING;



INSERT INTO operator_discount_limits (id, role_id, max_percent, max_amount, active, created_at, updated_at, version)

SELECT 'd2000000-0000-4000-8000-000000000002', r.id, 50.0000, NULL, TRUE,

       NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0

FROM roles r WHERE r.code = 'MANAGER'

ON CONFLICT (role_id) DO NOTHING;



INSERT INTO operator_discount_limits (id, role_id, max_percent, max_amount, active, created_at, updated_at, version)

SELECT 'd2000000-0000-4000-8000-000000000003', r.id, 100.0000, NULL, TRUE,

       NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0

FROM roles r WHERE r.code = 'ADMIN'

ON CONFLICT (role_id) DO NOTHING;



-- Política global padrão

INSERT INTO discount_policies (

    id, code, name, description, applies_to, max_percent, max_amount, priority, status, active,

    created_at, updated_at, version

) VALUES (

    'd3000000-0000-4000-8000-000000000001',

    'GLOBAL_DEFAULT',

    'Política global padrão',

    'Teto geral de desconto',

    'GLOBAL',

    100.0000,

    NULL,

    0,

    'ACTIVE',

    TRUE,

    NOW() AT TIME ZONE 'UTC',

    NOW() AT TIME ZONE 'UTC',

    0

)

ON CONFLICT (code) DO NOTHING;


