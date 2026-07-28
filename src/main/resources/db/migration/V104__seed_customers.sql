-- V104: clientes de exemplo
INSERT INTO customers (
    id, type, name, document, email, phone,
    zip_code, street, number, complement, district, city, state,
    active, created_at, updated_at, version
) VALUES
( 'e1000000-0000-4000-8000-000000000001', 'PF', 'Maria Silva', '52998224725',
  'maria.silva@example.com', '11999990001',
  '01310100', 'Av. Paulista', '1000', 'Sala 101', 'Bela Vista', 'São Paulo', 'SP',
  TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'e1000000-0000-4000-8000-000000000002', 'PJ', 'Tech Solutions LTDA', '11222333000181',
  'contato@techsolutions.example.com', '1133334444',
  '04538132', 'Rua Funchal', '418', NULL, 'Vila Olímpia', 'São Paulo', 'SP',
  TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'e1000000-0000-4000-8000-000000000003', 'PF', 'João Pereira', '39053344705',
  'joao.pereira@example.com', '21988887777',
  '20040020', 'Rua da Assembleia', '10', NULL, 'Centro', 'Rio de Janeiro', 'RJ',
  TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0);
