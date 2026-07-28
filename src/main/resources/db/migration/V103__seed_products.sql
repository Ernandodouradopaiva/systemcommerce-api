-- V103: produtos de exemplo
INSERT INTO products (id, sku, name, description, category_id, unit_price, cost_price, active, created_at, updated_at, version) VALUES
( 'd1000000-0000-4000-8000-000000000001', 'NB-001', 'Notebook 15"', 'Notebook corporativo 15 polegadas',
  'c1000000-0000-4000-8000-000000000001', 3500.00, 2800.00, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'd1000000-0000-4000-8000-000000000002', 'MS-001', 'Mouse óptico', 'Mouse USB com fio',
  'c1000000-0000-4000-8000-000000000001', 49.90, 22.00, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'd1000000-0000-4000-8000-000000000003', 'PAP-A4', 'Resma papel A4', 'Pacote com 500 folhas',
  'c1000000-0000-4000-8000-000000000002', 29.90, 18.50, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'd1000000-0000-4000-8000-000000000004', 'SRV-SUP', 'Hora de suporte', 'Hora técnica de suporte remoto',
  'c1000000-0000-4000-8000-000000000003', 120.00, 0.00, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0);
