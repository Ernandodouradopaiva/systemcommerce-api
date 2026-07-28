-- V107: complementa seeds de clientes com novos campos
UPDATE customers
SET trade_name = 'Maria S.',
    mobile = '11988880001',
    birth_date = DATE '1990-05-12',
    notes = 'Cliente PF seed',
    status = 'ACTIVE',
    active = TRUE
WHERE id = 'e1000000-0000-4000-8000-000000000001';

UPDATE customers
SET trade_name = 'Tech Solutions',
    state_registration = '123456789',
    mobile = '11977770002',
    notes = 'Cliente PJ seed',
    status = 'ACTIVE',
    active = TRUE
WHERE id = 'e1000000-0000-4000-8000-000000000002';

UPDATE customers
SET trade_name = NULL,
    mobile = '21977776666',
    birth_date = DATE '1985-11-03',
    notes = 'Cliente PF seed inativo para testes de filtro',
    status = 'INACTIVE',
    active = FALSE
WHERE id = 'e1000000-0000-4000-8000-000000000003';

-- Cliente adicional PJ
INSERT INTO customers (
    id, type, name, trade_name, document, state_registration,
    email, phone, mobile, birth_date, notes, status,
    zip_code, street, number, complement, district, city, state,
    active, created_at, updated_at, version
) VALUES (
    'e1000000-0000-4000-8000-000000000004',
    'PJ',
    'Comércio Mineiro SA',
    'Mineiro Store',
    '34028316000103',
    'ISENTO',
    'contato@mineiro.example.com',
    '3132221100',
    '31999991111',
    NULL,
    'Cliente PJ adicional',
    'ACTIVE',
    '30130000',
    'Av. Afonso Pena',
    '1500',
    NULL,
    'Centro',
    'Belo Horizonte',
    'MG',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
)
ON CONFLICT (id) DO NOTHING;
