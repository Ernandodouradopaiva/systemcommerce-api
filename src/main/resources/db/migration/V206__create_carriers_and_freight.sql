-- V206: Transportadoras, modalidades e frete (Prompt 73)
CREATE TABLE carriers (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    legal_name          VARCHAR(200)    NOT NULL,
    trade_name          VARCHAR(200)    NULL,
    document            VARCHAR(20)     NOT NULL,
    state_registration  VARCHAR(30)     NULL,
    antt_rntrc          VARCHAR(40)     NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    notes               VARCHAR(2000)   NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_carriers PRIMARY KEY (id),
    CONSTRAINT uk_carriers_org_code UNIQUE (organization_id, code),
    CONSTRAINT uk_carriers_org_document UNIQUE (organization_id, document),
    CONSTRAINT fk_carriers_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT ck_carriers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE carrier_contacts (
    id                  UUID            NOT NULL,
    carrier_id          UUID            NOT NULL,
    name                VARCHAR(150)    NOT NULL,
    phone               VARCHAR(30)     NULL,
    email               VARCHAR(255)    NULL,
    role_label          VARCHAR(80)     NULL,
    primary_contact     BOOLEAN         NOT NULL DEFAULT FALSE,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_carrier_contacts PRIMARY KEY (id),
    CONSTRAINT fk_cc_carrier FOREIGN KEY (carrier_id) REFERENCES carriers (id) ON DELETE CASCADE
);

CREATE TABLE freight_modes (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    name                VARCHAR(120)    NOT NULL,
    mode_type           VARCHAR(40)     NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_freight_modes PRIMARY KEY (id),
    CONSTRAINT uk_freight_modes UNIQUE (organization_id, code),
    CONSTRAINT fk_fm_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT ck_fm_type CHECK (mode_type IN (
        'OWN', 'CARRIER', 'PICKUP', 'MOTORBIKE', 'POSTAL', 'LOCAL', 'MARKETPLACE', 'OTHER'
    ))
);

CREATE TABLE freight_tables (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    carrier_id          UUID            NULL,
    freight_mode_id     UUID            NULL,
    name                VARCHAR(120)    NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    valid_from          DATE            NULL,
    valid_until         DATE            NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_freight_tables PRIMARY KEY (id),
    CONSTRAINT fk_ft_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_ft_carrier FOREIGN KEY (carrier_id) REFERENCES carriers (id),
    CONSTRAINT fk_ft_mode FOREIGN KEY (freight_mode_id) REFERENCES freight_modes (id)
);

CREATE TABLE freight_regions (
    id                  UUID            NOT NULL,
    freight_table_id    UUID            NOT NULL,
    region_code         VARCHAR(40)     NOT NULL,
    region_name         VARCHAR(120)    NULL,
    zip_from            VARCHAR(10)     NULL,
    zip_to              VARCHAR(10)     NULL,
    min_weight          NUMERIC(18, 4)  NULL,
    max_weight          NUMERIC(18, 4)  NULL,
    min_volume          NUMERIC(18, 4)  NULL,
    max_volume          NUMERIC(18, 4)  NULL,
    min_order_amount    NUMERIC(18, 2)  NULL,
    freight_amount      NUMERIC(18, 2)  NOT NULL,
    lead_time_days      INT             NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_freight_regions PRIMARY KEY (id),
    CONSTRAINT fk_fr_table FOREIGN KEY (freight_table_id) REFERENCES freight_tables (id) ON DELETE CASCADE,
    CONSTRAINT ck_fr_amount CHECK (freight_amount >= 0)
);

CREATE TABLE freight_quotations (
    id                  UUID            NOT NULL,
    organization_id     UUID            NOT NULL,
    store_id            UUID            NULL,
    carrier_id          UUID            NULL,
    freight_mode_id     UUID            NULL,
    sales_order_id      UUID            NULL,
    quote_id            UUID            NULL,
    zip_code            VARCHAR(10)     NULL,
    weight              NUMERIC(18, 4)  NULL,
    volume              NUMERIC(18, 4)  NULL,
    order_amount        NUMERIC(18, 2)  NULL,
    calculated_amount   NUMERIC(18, 2)  NOT NULL,
    manual_override     BOOLEAN         NOT NULL DEFAULT FALSE,
    override_amount     NUMERIC(18, 2)  NULL,
    source              VARCHAR(40)     NOT NULL DEFAULT 'TABLE',
    calculated_at       TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    calculated_by       UUID            NULL,
    notes               VARCHAR(1000)   NULL,
    CONSTRAINT pk_freight_quotations PRIMARY KEY (id),
    CONSTRAINT fk_fq_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_fq_carrier FOREIGN KEY (carrier_id) REFERENCES carriers (id),
    CONSTRAINT fk_fq_mode FOREIGN KEY (freight_mode_id) REFERENCES freight_modes (id),
    CONSTRAINT ck_fq_source CHECK (source IN ('TABLE', 'MANUAL', 'EXTERNAL'))
);

ALTER TABLE shipments
    ADD COLUMN IF NOT EXISTS carrier_id UUID NULL,
    ADD COLUMN IF NOT EXISTS freight_mode_id UUID NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_shipments_carrier') THEN
        ALTER TABLE shipments ADD CONSTRAINT fk_shipments_carrier FOREIGN KEY (carrier_id) REFERENCES carriers (id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_shipments_freight_mode') THEN
        ALTER TABLE shipments ADD CONSTRAINT fk_shipments_freight_mode FOREIGN KEY (freight_mode_id) REFERENCES freight_modes (id);
    END IF;
END $$;

ALTER TABLE sales_orders
    ADD COLUMN IF NOT EXISTS carrier_id UUID NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_so_carrier') THEN
        ALTER TABLE sales_orders ADD CONSTRAINT fk_so_carrier FOREIGN KEY (carrier_id) REFERENCES carriers (id);
    END IF;
END $$;
