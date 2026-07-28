-- V109: motivos de ajuste de estoque
INSERT INTO inventory_adjustment_reasons (id, code, description, active, created_at, updated_at, version) VALUES
('f3000000-0000-4000-8000-000000000001', 'INVENTORY_COUNT', 'Contagem de inventário', TRUE,
 NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
('f3000000-0000-4000-8000-000000000002', 'DAMAGE', 'Avaria / perda', TRUE,
 NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
('f3000000-0000-4000-8000-000000000003', 'DONATION', 'Doação', TRUE,
 NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
('f3000000-0000-4000-8000-000000000004', 'SUPPLIER_RETURN', 'Devolução a fornecedor', TRUE,
 NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
('f3000000-0000-4000-8000-000000000005', 'CORRECTION', 'Correção de saldo', TRUE,
 NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0);
