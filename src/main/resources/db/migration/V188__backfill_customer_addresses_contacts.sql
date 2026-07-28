-- V188: backfill de endereço/contato flat (customers) → tabelas filhas (Prompt 58)
-- Não remove as colunas flat de customers (mantidas por compatibilidade; ver DOCUMENTATION.md).

INSERT INTO customer_addresses (
    id, customer_id, type, zip_code, street, number, complement, district, city, state,
    is_default, active, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), c.id, 'COMMERCIAL', c.zip_code, c.street, c.number, c.complement, c.district, c.city, c.state,
    TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM customers c
WHERE (c.street IS NOT NULL OR c.zip_code IS NOT NULL OR c.city IS NOT NULL)
  AND NOT EXISTS (
      SELECT 1 FROM customer_addresses a WHERE a.customer_id = c.id
  );

INSERT INTO customer_contacts (
    id, customer_id, type, name, email, phone, mobile, is_default, active, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), c.id, 'GENERAL', c.name, c.email, c.phone, c.mobile, TRUE, TRUE,
    NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM customers c
WHERE (c.email IS NOT NULL OR c.phone IS NOT NULL OR c.mobile IS NOT NULL)
  AND NOT EXISTS (
      SELECT 1 FROM customer_contacts ct WHERE ct.customer_id = c.id
  );

-- Histórico inicial de status para clientes já existentes (marco de início do módulo de histórico)
INSERT INTO customer_status_history (id, customer_id, previous_status, new_status, reason, changed_at)
SELECT gen_random_uuid(), c.id, NULL, c.status, 'Situação inicial registrada pela migração do Prompt 58', c.updated_at
FROM customers c
WHERE NOT EXISTS (
    SELECT 1 FROM customer_status_history h WHERE h.customer_id = c.id
);
