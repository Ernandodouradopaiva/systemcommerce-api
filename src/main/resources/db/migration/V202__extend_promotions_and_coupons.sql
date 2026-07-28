-- V202: Motor de promoções e cupons (Prompt 69) — estende promotions existentes
ALTER TABLE promotions
    ADD COLUMN IF NOT EXISTS promotion_type VARCHAR(40) NULL,
    ADD COLUMN IF NOT EXISTS priority INT NOT NULL DEFAULT 100,
    ADD COLUMN IF NOT EXISTS stackable BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS min_order_amount NUMERIC(18, 2) NULL,
    ADD COLUMN IF NOT EXISTS brand_id UUID NULL,
    ADD COLUMN IF NOT EXISTS category_id UUID NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_promotions_brand') THEN
        ALTER TABLE promotions ADD CONSTRAINT fk_promotions_brand FOREIGN KEY (brand_id) REFERENCES brands (id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_promotions_category') THEN
        ALTER TABLE promotions ADD CONSTRAINT fk_promotions_category FOREIGN KEY (category_id) REFERENCES categories (id);
    END IF;
END $$;

CREATE TABLE promotion_rules (
    id                  UUID            NOT NULL,
    promotion_id        UUID            NOT NULL,
    rule_type           VARCHAR(40)     NOT NULL,
    config_json         TEXT            NULL,
    sort_order          INT             NOT NULL DEFAULT 0,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_promotion_rules PRIMARY KEY (id),
    CONSTRAINT fk_prule_promo FOREIGN KEY (promotion_id) REFERENCES promotions (id) ON DELETE CASCADE,
    CONSTRAINT ck_prule_type CHECK (rule_type IN (
        'PERCENT_DISCOUNT', 'FIXED_DISCOUNT', 'PROMO_PRICE', 'BUY_QTY_DISCOUNT',
        'BUY_X_PAY_Y', 'CATEGORY', 'BRAND', 'MIN_AMOUNT', 'CUSTOMER_GROUP', 'COUPON'
    ))
);

CREATE TABLE promotion_conditions (
    id                  UUID            NOT NULL,
    promotion_id        UUID            NOT NULL,
    condition_type      VARCHAR(40)     NOT NULL,
    reference_id        UUID            NULL,
    min_quantity        NUMERIC(18, 4)  NULL,
    min_amount          NUMERIC(18, 2)  NULL,
    config_json         TEXT            NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_promotion_conditions PRIMARY KEY (id),
    CONSTRAINT fk_pcond_promo FOREIGN KEY (promotion_id) REFERENCES promotions (id) ON DELETE CASCADE
);

CREATE TABLE promotion_benefits (
    id                  UUID            NOT NULL,
    promotion_id        UUID            NOT NULL,
    benefit_type        VARCHAR(40)     NOT NULL,
    percent_value       NUMERIC(7, 4)   NULL,
    fixed_value         NUMERIC(18, 2)  NULL,
    promo_unit_price    NUMERIC(18, 4)  NULL,
    buy_quantity        NUMERIC(18, 4)  NULL,
    pay_quantity        NUMERIC(18, 4)  NULL,
    max_benefit_amount  NUMERIC(18, 2)  NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_promotion_benefits PRIMARY KEY (id),
    CONSTRAINT fk_pben_promo FOREIGN KEY (promotion_id) REFERENCES promotions (id) ON DELETE CASCADE
);

CREATE TABLE coupons (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    promotion_id        UUID            NULL,
    code                VARCHAR(60)     NOT NULL,
    description         VARCHAR(500)    NULL,
    max_uses            INT             NULL,
    max_uses_per_customer INT           NULL,
    used_count          INT             NOT NULL DEFAULT 0,
    valid_from          TIMESTAMPTZ     NULL,
    valid_until         TIMESTAMPTZ     NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_coupons PRIMARY KEY (id),
    CONSTRAINT uk_coupons_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_coupons_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_coupons_promo FOREIGN KEY (promotion_id) REFERENCES promotions (id),
    CONSTRAINT ck_coupons_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXHAUSTED'))
);

CREATE TABLE promotion_applications (
    id                  UUID            NOT NULL,
    promotion_id        UUID            NOT NULL,
    sale_id             UUID            NULL,
    sales_order_id      UUID            NULL,
    quote_id            UUID            NULL,
    coupon_id           UUID            NULL,
    benefit_amount      NUMERIC(18, 2)  NOT NULL DEFAULT 0,
    snapshot_json       TEXT            NULL,
    applied_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    applied_by          UUID            NULL,
    CONSTRAINT pk_promotion_applications PRIMARY KEY (id),
    CONSTRAINT fk_pa_promo FOREIGN KEY (promotion_id) REFERENCES promotions (id),
    CONSTRAINT fk_pa_sale FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT fk_pa_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id)
);

CREATE INDEX idx_coupons_code ON coupons (organization_id, code);
CREATE INDEX idx_pa_sale ON promotion_applications (sale_id);
