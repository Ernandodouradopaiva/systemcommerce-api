-- V128: permissões de venda rápida do PDV

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)

VALUES

(

    'a1000000-0000-4000-8000-00000000002b',

    'POS_SALE_CREATE',

    'Criar e operar venda rápida no PDV',

    'POS',

    'Iniciar venda, incluir itens, identificar cliente e consultar resumo no PDV',

    TRUE,

    NOW() AT TIME ZONE 'UTC',

    NOW() AT TIME ZONE 'UTC',

    0

),

(

    'a1000000-0000-4000-8000-00000000002c',

    'POS_SALE_ITEM_REMOVE',

    'Remover item da venda no PDV',

    'POS',

    'Remover ou cancelar item da venda rápida',

    TRUE,

    NOW() AT TIME ZONE 'UTC',

    NOW() AT TIME ZONE 'UTC',

    0

),

(

    'a1000000-0000-4000-8000-00000000002d',

    'POS_SALE_DISCOUNT',

    'Aplicar desconto na venda do PDV',

    'POS',

    'Aplicar desconto em item ou geral até o limite do operador',

    TRUE,

    NOW() AT TIME ZONE 'UTC',

    NOW() AT TIME ZONE 'UTC',

    0

),

(

    'a1000000-0000-4000-8000-00000000002e',

    'POS_SALE_HIGH_DISCOUNT',

    'Autorizar desconto elevado no PDV',

    'POS',

    'Autorizar desconto acima do limite padrão do operador',

    TRUE,

    NOW() AT TIME ZONE 'UTC',

    NOW() AT TIME ZONE 'UTC',

    0

),

(

    'a1000000-0000-4000-8000-00000000002f',

    'POS_SALE_SUSPEND',

    'Suspender e recuperar venda no PDV',

    'POS',

    'Suspender venda e recuperar venda suspensa',

    TRUE,

    NOW() AT TIME ZONE 'UTC',

    NOW() AT TIME ZONE 'UTC',

    0

),

(

    'a1000000-0000-4000-8000-000000000030',

    'POS_SALE_CANCEL',

    'Cancelar ou descartar venda no PDV',

    'POS',

    'Descartar rascunho ou cancelar venda do PDV',

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

WHERE r.code = 'ADMIN'

  AND p.code IN (

      'POS_SALE_CREATE', 'POS_SALE_ITEM_REMOVE', 'POS_SALE_DISCOUNT',

      'POS_SALE_HIGH_DISCOUNT', 'POS_SALE_SUSPEND', 'POS_SALE_CANCEL'

  )

  AND NOT EXISTS (

      SELECT 1 FROM role_permissions rp

      WHERE rp.role_id = r.id AND rp.permission_id = p.id

  );



INSERT INTO role_permissions (role_id, permission_id, created_at)

SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'

FROM roles r

CROSS JOIN permissions p

WHERE r.code = 'MANAGER'

  AND p.code IN (

      'POS_SALE_CREATE', 'POS_SALE_ITEM_REMOVE', 'POS_SALE_DISCOUNT',

      'POS_SALE_HIGH_DISCOUNT', 'POS_SALE_SUSPEND', 'POS_SALE_CANCEL'

  )

  AND NOT EXISTS (

      SELECT 1 FROM role_permissions rp

      WHERE rp.role_id = r.id AND rp.permission_id = p.id

  );



INSERT INTO role_permissions (role_id, permission_id, created_at)

SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'

FROM roles r

CROSS JOIN permissions p

WHERE r.code = 'SELLER'

  AND p.code IN (

      'POS_SALE_CREATE', 'POS_SALE_ITEM_REMOVE', 'POS_SALE_DISCOUNT',

      'POS_SALE_SUSPEND', 'POS_SALE_CANCEL'

  )

  AND NOT EXISTS (

      SELECT 1 FROM role_permissions rp

      WHERE rp.role_id = r.id AND rp.permission_id = p.id

  );


