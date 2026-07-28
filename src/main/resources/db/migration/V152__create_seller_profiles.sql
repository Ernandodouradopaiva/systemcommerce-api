-- V152: perfis de vendedor e autorização comercial por loja
CREATE TABLE seller_profiles (
    id                          UUID            NOT NULL,
    organization_id             UUID            NOT NULL,
    employee_id                 UUID            NOT NULL,
    seller_code                 VARCHAR(40)     NOT NULL,
    status                      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    max_discount_percent        NUMERIC(7, 4)   NOT NULL DEFAULT 0,
    allows_external_sale        BOOLEAN         NOT NULL DEFAULT FALSE,
    allows_other_stores         BOOLEAN         NOT NULL DEFAULT FALSE,
    monthly_target_amount       NUMERIC(19, 2)  NULL,
    supervisor_employee_id      UUID            NULL,
    enabled_at                  DATE            NOT NULL DEFAULT CURRENT_DATE,
    disabled_at                 DATE            NULL,
    notes                       VARCHAR(2000)   NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_seller_profiles PRIMARY KEY (id),
    CONSTRAINT fk_seller_profiles_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_seller_profiles_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_seller_profiles_supervisor FOREIGN KEY (supervisor_employee_id) REFERENCES employees (id),
    CONSTRAINT uk_seller_profiles_employee UNIQUE (employee_id),
    CONSTRAINT uk_seller_profiles_org_code UNIQUE (organization_id, seller_code),
    CONSTRAINT ck_seller_profiles_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_seller_profiles_discount CHECK (max_discount_percent >= 0 AND max_discount_percent <= 100),
    CONSTRAINT ck_seller_profiles_code_not_blank CHECK (LENGTH(TRIM(seller_code)) > 0)
);

CREATE INDEX idx_seller_profiles_org ON seller_profiles (organization_id);
CREATE INDEX idx_seller_profiles_status ON seller_profiles (status);
CREATE INDEX idx_seller_profiles_code ON seller_profiles (seller_code);

CREATE TABLE seller_store_assignments (
    id                          UUID            NOT NULL,
    seller_profile_id           UUID            NOT NULL,
    store_id                    UUID            NOT NULL,
    start_date                  DATE            NOT NULL,
    end_date                    DATE            NULL,
    primary_assignment          BOOLEAN         NOT NULL DEFAULT FALSE,
    temporary_assignment        BOOLEAN         NOT NULL DEFAULT FALSE,
    allows_register_sale        BOOLEAN         NOT NULL DEFAULT TRUE,
    max_discount_percent        NUMERIC(7, 4)   NULL,
    target_amount               NUMERIC(19, 2)  NULL,
    status                      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    notes                       VARCHAR(2000)   NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_seller_store_assignments PRIMARY KEY (id),
    CONSTRAINT fk_ssa_seller FOREIGN KEY (seller_profile_id) REFERENCES seller_profiles (id),
    CONSTRAINT fk_ssa_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT ck_ssa_status CHECK (status IN ('ACTIVE', 'ENDED', 'REVOKED')),
    CONSTRAINT ck_ssa_period CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_ssa_temp_period CHECK (
        temporary_assignment = FALSE OR end_date IS NOT NULL
    ),
    CONSTRAINT ck_ssa_discount CHECK (
        max_discount_percent IS NULL
        OR (max_discount_percent >= 0 AND max_discount_percent <= 100)
    )
);

CREATE INDEX idx_ssa_seller ON seller_store_assignments (seller_profile_id);
CREATE INDEX idx_ssa_store ON seller_store_assignments (store_id);
CREATE INDEX idx_ssa_status ON seller_store_assignments (status);
CREATE INDEX idx_ssa_primary ON seller_store_assignments (seller_profile_id, primary_assignment)
    WHERE primary_assignment = TRUE AND status = 'ACTIVE';

ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS seller_profile_id UUID NULL;

ALTER TABLE sales
    DROP CONSTRAINT IF EXISTS fk_sales_seller_profile;

ALTER TABLE sales
    ADD CONSTRAINT fk_sales_seller_profile
        FOREIGN KEY (seller_profile_id) REFERENCES seller_profiles (id);

CREATE INDEX IF NOT EXISTS idx_sales_seller_profile ON sales (seller_profile_id);

COMMENT ON TABLE seller_profiles IS 'Habilitação comercial do profissional (Seller); distinto de User e de lotação RH';
COMMENT ON TABLE seller_store_assignments IS 'Autorização comercial por loja (histórico; não apagar)';
COMMENT ON COLUMN sales.seller_profile_id IS 'Vendedor comercial no momento da venda (imutável após registro)';

-- Seed: EMP-0001 como vendedor VEND-0001 na LOJA-01
INSERT INTO seller_profiles (
    id, organization_id, employee_id, seller_code, status, max_discount_percent,
    allows_external_sale, allows_other_stores, enabled_at, active, created_at, updated_at, version
)
SELECT
    'b3000000-0000-4000-8000-000000000001',
    e.organization_id,
    e.id,
    'VEND-0001',
    'ACTIVE',
    10.0000,
    FALSE,
    FALSE,
    CURRENT_DATE,
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM employees e
WHERE e.registration_number = 'EMP-0001'
ON CONFLICT (id) DO NOTHING;

INSERT INTO seller_store_assignments (
    id, seller_profile_id, store_id, start_date, end_date,
    primary_assignment, temporary_assignment, allows_register_sale,
    max_discount_percent, status, active, created_at, updated_at, version
)
SELECT
    'b3000000-0000-4000-8000-000000000002',
    sp.id,
    s.id,
    CURRENT_DATE,
    NULL,
    TRUE,
    FALSE,
    TRUE,
    NULL,
    'ACTIVE',
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM seller_profiles sp
JOIN stores s ON s.code = 'LOJA-01' AND s.organization_id = sp.organization_id
WHERE sp.seller_code = 'VEND-0001'
  AND NOT EXISTS (
      SELECT 1 FROM seller_store_assignments x WHERE x.id = 'b3000000-0000-4000-8000-000000000002'
  );
