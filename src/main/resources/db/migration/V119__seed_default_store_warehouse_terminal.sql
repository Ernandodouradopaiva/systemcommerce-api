-- V119: seeds de loja, depósito e terminal padrão (idempotente)
INSERT INTO stores (
    id, code, name, trade_name, timezone, status, active,
    created_at, updated_at, version
) VALUES (
    'c1000000-0000-4000-8000-000000000001',
    'LOJA-01',
    'Loja Principal',
    'SystemCommerce',
    'America/Sao_Paulo',
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO warehouses (
    id, store_id, code, name, allows_sale, status, active,
    created_at, updated_at, version
)
SELECT
    'c1000000-0000-4000-8000-000000000002',
    s.id,
    'DEP-01',
    'Depósito Principal',
    TRUE,
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM stores s
WHERE s.code = 'LOJA-01'
ON CONFLICT (store_id, code) DO NOTHING;

INSERT INTO pos_terminals (
    id, store_id, warehouse_id, code, name, terminal_number,
    status, station_identifier, printer_name, print_model,
    active, created_at, updated_at, version
)
SELECT
    'c1000000-0000-4000-8000-000000000003',
    s.id,
    w.id,
    'TERM-01',
    'Terminal PDV 01',
    1,
    'ACTIVE',
    'STATION-01',
    NULL,
    'NONE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM stores s
JOIN warehouses w ON w.store_id = s.id AND w.code = 'DEP-01'
WHERE s.code = 'LOJA-01'
ON CONFLICT (store_id, code) DO NOTHING;
