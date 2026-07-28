-- V134: permissões de checkout/pagamento PDV

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)

VALUES

(

    'a1000000-0000-4000-8000-000000000036',

    'POS_PAYMENT_MANAGE',

    'Gerir pagamentos no PDV',

    'POS',

    'Adicionar, confirmar, remover pendente e consultar pagamentos da venda no PDV',

    TRUE,

    NOW() AT TIME ZONE 'UTC',

    NOW() AT TIME ZONE 'UTC',

    0

),

(

    'a1000000-0000-4000-8000-000000000037',

    'POS_PAYMENT_REFUND',

    'Estornar pagamento no PDV',

    'POS',

    'Estornar pagamento confirmado da venda PDV',

    TRUE,

    NOW() AT TIME ZONE 'UTC',

    NOW() AT TIME ZONE 'UTC',

    0

),

(

    'a1000000-0000-4000-8000-000000000038',

    'POS_SALE_FINALIZE',

    'Finalizar venda no PDV',

    'POS',

    'Confirmar venda, pagamentos e emitir dados de comprovante',

    TRUE,

    NOW() AT TIME ZONE 'UTC',

    NOW() AT TIME ZONE 'UTC',

    0

)

ON CONFLICT (code) DO NOTHING;



INSERT INTO role_permissions (role_id, permission_id, created_at)

SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'

FROM roles r CROSS JOIN permissions p

WHERE r.code IN ('ADMIN', 'MANAGER', 'SELLER')

  AND p.code IN ('POS_PAYMENT_MANAGE', 'POS_SALE_FINALIZE')

  AND NOT EXISTS (

      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id

  );



INSERT INTO role_permissions (role_id, permission_id, created_at)

SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'

FROM roles r CROSS JOIN permissions p

WHERE r.code IN ('ADMIN', 'MANAGER')

  AND p.code = 'POS_PAYMENT_REFUND'

  AND NOT EXISTS (

      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id

  );


