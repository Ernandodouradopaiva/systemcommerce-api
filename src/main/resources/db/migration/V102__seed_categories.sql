-- V102: categorias de exemplo
INSERT INTO categories (id, name, description, active, created_at, updated_at, version) VALUES
( 'c1000000-0000-4000-8000-000000000001', 'Informática', 'Equipamentos e acessórios de informática', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'c1000000-0000-4000-8000-000000000002', 'Escritório', 'Materiais de escritório e papelaria', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'c1000000-0000-4000-8000-000000000003', 'Serviços', 'Serviços avulsos comercializáveis', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0);
