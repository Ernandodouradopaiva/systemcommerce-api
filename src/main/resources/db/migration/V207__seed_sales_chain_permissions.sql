-- V207: Permissões Prompts 64–73
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000190', 'BRAND_READ', 'Consultar marcas', 'CATALOG', 'Consultar marcas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000191', 'BRAND_MANAGE', 'Gerenciar marcas', 'CATALOG', 'CRUD marcas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000192', 'MANUFACTURER_READ', 'Consultar fabricantes', 'CATALOG', 'Consultar fabricantes', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000193', 'MANUFACTURER_MANAGE', 'Gerenciar fabricantes', 'CATALOG', 'CRUD fabricantes', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000194', 'PRODUCT_LINE_READ', 'Consultar linhas', 'CATALOG', 'Consultar linhas de produto', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000195', 'PRODUCT_LINE_MANAGE', 'Gerenciar linhas', 'CATALOG', 'CRUD linhas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000196', 'UOM_READ', 'Consultar unidades', 'CATALOG', 'Unidades de medida', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000197', 'UOM_MANAGE', 'Gerenciar unidades', 'CATALOG', 'CRUD UOM e conversões', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000198', 'STORAGE_LOCATION_READ', 'Consultar localizações', 'INVENTORY', 'Localizações físicas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000199', 'STORAGE_LOCATION_MANAGE', 'Gerenciar localizações', 'INVENTORY', 'CRUD endereçamento', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000200', 'STOCK_RESERVATION_READ', 'Consultar reservas', 'INVENTORY', 'Reservas de estoque', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000201', 'STOCK_RESERVATION_MANAGE', 'Gerenciar reservas', 'INVENTORY', 'Criar/liberar/consumir reservas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000202', 'PICKING_READ', 'Consultar separações', 'SALES', 'Picking', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000203', 'PICKING_MANAGE', 'Gerenciar separações', 'SALES', 'Operar picking', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000204', 'SHIPMENT_READ', 'Consultar expedições', 'SALES', 'Expedição', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000205', 'SHIPMENT_MANAGE', 'Gerenciar expedições', 'SALES', 'Operar expedição', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000206', 'CARRIER_READ', 'Consultar transportadoras', 'LOGISTICS', 'Transportadoras', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000207', 'CARRIER_MANAGE', 'Gerenciar transportadoras', 'LOGISTICS', 'CRUD transportadoras/frete', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000208', 'COUPON_READ', 'Consultar cupons', 'PRICING', 'Cupons', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000209', 'COUPON_MANAGE', 'Gerenciar cupons', 'PRICING', 'CRUD cupons', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000210', 'QUOTE_FORCE_CONVERT_EXPIRED', 'Converter orçamento expirado', 'SALES', 'Autoriza conversão de orçamento expirado', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'BRAND_READ', 'BRAND_MANAGE', 'MANUFACTURER_READ', 'MANUFACTURER_MANAGE',
      'PRODUCT_LINE_READ', 'PRODUCT_LINE_MANAGE', 'UOM_READ', 'UOM_MANAGE',
      'STORAGE_LOCATION_READ', 'STORAGE_LOCATION_MANAGE',
      'STOCK_RESERVATION_READ', 'STOCK_RESERVATION_MANAGE',
      'PICKING_READ', 'PICKING_MANAGE', 'SHIPMENT_READ', 'SHIPMENT_MANAGE',
      'CARRIER_READ', 'CARRIER_MANAGE', 'COUPON_READ', 'COUPON_MANAGE',
      'QUOTE_FORCE_CONVERT_EXPIRED'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
