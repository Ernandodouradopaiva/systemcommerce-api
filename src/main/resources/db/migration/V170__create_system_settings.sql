-- V170: configurações hierárquicas (organização / grupo / loja / terminal / usuário)

CREATE TABLE IF NOT EXISTS system_settings (
    id UUID PRIMARY KEY,
    setting_key VARCHAR(80) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    organization_id UUID REFERENCES organizations (id),
    store_group_id UUID REFERENCES store_groups (id),
    store_id UUID REFERENCES stores (id),
    terminal_id UUID REFERENCES pos_terminals (id),
    user_id UUID REFERENCES users (id),
    value_text TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_system_settings_scope CHECK (scope IN (
        'ORGANIZATION', 'STORE_GROUP', 'STORE', 'TERMINAL', 'USER'
    )),
    CONSTRAINT ck_system_settings_scope_refs CHECK (
        (scope = 'ORGANIZATION' AND organization_id IS NOT NULL
            AND store_group_id IS NULL AND store_id IS NULL AND terminal_id IS NULL AND user_id IS NULL)
        OR (scope = 'STORE_GROUP' AND store_group_id IS NOT NULL
            AND store_id IS NULL AND terminal_id IS NULL AND user_id IS NULL)
        OR (scope = 'STORE' AND store_id IS NOT NULL
            AND terminal_id IS NULL AND user_id IS NULL)
        OR (scope = 'TERMINAL' AND terminal_id IS NOT NULL AND user_id IS NULL)
        OR (scope = 'USER' AND user_id IS NOT NULL
            AND store_id IS NULL AND terminal_id IS NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_system_settings_org
    ON system_settings (setting_key, organization_id)
    WHERE active = TRUE AND scope = 'ORGANIZATION';

CREATE UNIQUE INDEX IF NOT EXISTS uq_system_settings_store_group
    ON system_settings (setting_key, store_group_id)
    WHERE active = TRUE AND scope = 'STORE_GROUP';

CREATE UNIQUE INDEX IF NOT EXISTS uq_system_settings_store
    ON system_settings (setting_key, store_id)
    WHERE active = TRUE AND scope = 'STORE';

CREATE UNIQUE INDEX IF NOT EXISTS uq_system_settings_terminal
    ON system_settings (setting_key, terminal_id)
    WHERE active = TRUE AND scope = 'TERMINAL';

CREATE UNIQUE INDEX IF NOT EXISTS uq_system_settings_user
    ON system_settings (setting_key, user_id)
    WHERE active = TRUE AND scope = 'USER';

CREATE INDEX IF NOT EXISTS idx_system_settings_key ON system_settings (setting_key);
CREATE INDEX IF NOT EXISTS idx_system_settings_store ON system_settings (store_id) WHERE store_id IS NOT NULL;

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000134', 'SYSTEM_SETTING_READ', 'Consultar configurações do sistema', 'SETTINGS',
     'Consultar configurações hierárquicas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000135', 'SYSTEM_SETTING_MANAGE', 'Gerenciar configurações do sistema', 'SETTINGS',
     'Criar e alterar configurações hierárquicas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN ('SYSTEM_SETTING_READ', 'SYSTEM_SETTING_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Defaults organizacionais
INSERT INTO system_settings (
    id, setting_key, scope, organization_id, value_text, active, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    key_name,
    'ORGANIZATION',
    'b1000000-0000-4000-8000-000000000001',
    default_value,
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM (VALUES
    ('REQUIRE_SELLER', 'false'),
    ('ALLOW_NEGATIVE_STOCK', 'false'),
    ('DEFAULT_WAREHOUSE', ''),
    ('REQUIRE_CUSTOMER_ON_SALE', 'false'),
    ('UI_THEME', 'light')
) AS defs(key_name, default_value)
WHERE NOT EXISTS (
    SELECT 1 FROM system_settings s
    WHERE s.setting_key = defs.key_name
      AND s.scope = 'ORGANIZATION'
      AND s.organization_id = 'b1000000-0000-4000-8000-000000000001'
      AND s.active = TRUE
);

COMMENT ON TABLE system_settings IS 'Configurações hierárquicas ERP/multiloja (terminal > loja > grupo > organização)';
