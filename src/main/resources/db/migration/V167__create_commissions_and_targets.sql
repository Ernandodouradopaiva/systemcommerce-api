-- V167: metas de vendas, políticas de comissão, cálculos e ajustes

CREATE TABLE IF NOT EXISTS sales_targets (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    seller_profile_id UUID REFERENCES seller_profiles (id),
    store_id UUID REFERENCES stores (id),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    category_id UUID REFERENCES categories (id),
    product_id UUID REFERENCES products (id),
    target_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    target_quantity NUMERIC(19, 3) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_sales_targets_status CHECK (status IN ('ACTIVE', 'CLOSED')),
    CONSTRAINT ck_sales_targets_period CHECK (period_end >= period_start)
);

CREATE INDEX IF NOT EXISTS idx_sales_targets_org_period ON sales_targets (organization_id, period_start, period_end);
CREATE INDEX IF NOT EXISTS idx_sales_targets_seller ON sales_targets (seller_profile_id, period_start);

CREATE TABLE IF NOT EXISTS commission_policies (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(200) NOT NULL,
    policy_version INTEGER NOT NULL DEFAULT 1,
    store_id UUID REFERENCES stores (id),
    seller_profile_id UUID REFERENCES seller_profiles (id),
    product_id UUID REFERENCES products (id),
    category_id UUID REFERENCES categories (id),
    channel VARCHAR(20) NOT NULL DEFAULT 'ANY',
    percent NUMERIC(7, 4) NOT NULL DEFAULT 0,
    fixed_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    requires_paid BOOLEAN NOT NULL DEFAULT FALSE,
    applies_on_confirmed BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from TIMESTAMPTZ,
    valid_to TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_commission_policies_org_code UNIQUE (organization_id, code),
    CONSTRAINT ck_commission_policies_channel CHECK (channel IN ('ADMIN', 'POS', 'ANY')),
    CONSTRAINT ck_commission_policies_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_commission_policies_org_status ON commission_policies (organization_id, status);

CREATE TABLE IF NOT EXISTS commission_calculations (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sales (id),
    sale_item_id UUID REFERENCES sale_items (id),
    seller_profile_id UUID NOT NULL REFERENCES seller_profiles (id),
    store_id UUID REFERENCES stores (id),
    policy_id UUID NOT NULL REFERENCES commission_policies (id),
    policy_version INTEGER NOT NULL,
    base_amount NUMERIC(19, 2) NOT NULL,
    commission_amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CALCULATED',
    calculated_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_commission_calculations_status CHECK (status IN ('CALCULATED', 'REVERSED', 'ADJUSTED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_commission_calc_sale_item_policy
    ON commission_calculations (
        sale_id,
        policy_id,
        COALESCE(sale_item_id, '00000000-0000-0000-0000-000000000000'::uuid)
    );

CREATE INDEX IF NOT EXISTS idx_commission_calculations_seller ON commission_calculations (seller_profile_id, calculated_at);
CREATE INDEX IF NOT EXISTS idx_commission_calculations_sale ON commission_calculations (sale_id);

CREATE TABLE IF NOT EXISTS commission_adjustments (
    id UUID PRIMARY KEY,
    calculation_id UUID NOT NULL REFERENCES commission_calculations (id),
    amount NUMERIC(19, 2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_commission_adjustments_calc ON commission_adjustments (calculation_id);

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000125', 'SALES_TARGET_READ', 'Consultar metas de vendas', 'COMMISSION',
     'Consultar metas de vendas por loja/vendedor', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000126', 'SALES_TARGET_MANAGE', 'Gerenciar metas de vendas', 'COMMISSION',
     'Criar e alterar metas de vendas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000127', 'COMMISSION_READ', 'Consultar comissões', 'COMMISSION',
     'Consultar políticas e cálculos de comissão', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000128', 'COMMISSION_MANAGE', 'Gerenciar comissões', 'COMMISSION',
     'Cadastrar políticas e calcular comissões', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000129', 'COMMISSION_CLOSE_PERIOD', 'Fechar período de comissão', 'COMMISSION',
     'Encerrar metas e período de comissão', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000130', 'SELLER_VIEW_OWN_COMMISSION', 'Ver próprias comissões', 'COMMISSION',
     'Vendedor consulta apenas suas comissões', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'SALES_TARGET_READ', 'SALES_TARGET_MANAGE',
      'COMMISSION_READ', 'COMMISSION_MANAGE', 'COMMISSION_CLOSE_PERIOD',
      'SELLER_VIEW_OWN_COMMISSION'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SELLER'
  AND p.code = 'SELLER_VIEW_OWN_COMMISSION'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
