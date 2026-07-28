-- V166: histórico de vendedor na venda + vendedor padrão do terminal + permissões

CREATE TABLE IF NOT EXISTS sale_seller_history (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sales (id),
    previous_seller_profile_id UUID REFERENCES seller_profiles (id),
    new_seller_profile_id UUID REFERENCES seller_profiles (id),
    previous_seller_code VARCHAR(40),
    new_seller_code VARCHAR(40),
    previous_seller_name VARCHAR(200),
    new_seller_name VARCHAR(200),
    changed_by UUID REFERENCES users (id),
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by UUID
);

CREATE INDEX IF NOT EXISTS idx_sale_seller_history_sale ON sale_seller_history (sale_id, created_at);

ALTER TABLE pos_terminals
    ADD COLUMN IF NOT EXISTS default_seller_profile_id UUID REFERENCES seller_profiles (id);

ALTER TABLE stores
    ADD COLUMN IF NOT EXISTS require_seller_admin BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS require_seller_pos BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000120', 'SALE_SELLER_SELECT', 'Selecionar vendedor na venda', 'SALE',
     'Selecionar vendedor em rascunho', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000121', 'SALE_SELLER_CHANGE', 'Trocar vendedor na venda', 'SALE',
     'Trocar vendedor antes da confirmação', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000122', 'SALE_SELLER_CORRECT', 'Corrigir vendedor pós-confirmação', 'SALE',
     'Correção administrativa do vendedor', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000123', 'POS_SELLER_SELECT', 'Selecionar vendedor no PDV', 'POS',
     'Selecionar/trocar vendedor no PDV', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000124', 'POS_MULTI_SESSION', 'Múltiplas sessões do mesmo operador', 'POS',
     'Permitir operador com mais de uma sessão aberta', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN ('SALE_SELLER_SELECT', 'SALE_SELLER_CHANGE', 'SALE_SELLER_CORRECT', 'POS_SELLER_SELECT', 'POS_MULTI_SESSION')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'CASHIER'
  AND p.code IN ('SALE_SELLER_SELECT', 'POS_SELLER_SELECT')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
