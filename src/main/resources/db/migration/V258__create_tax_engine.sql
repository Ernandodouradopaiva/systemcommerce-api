-- V258: Motor de tributação (Prompt 127)
CREATE TABLE tax_rules (
    id                  UUID            NOT NULL,
    organization_id     UUID            NULL,
    code                VARCHAR(60)     NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    description         VARCHAR(500)    NULL,
    tax_kind            VARCHAR(40)     NOT NULL,
    priority            INT             NOT NULL DEFAULT 0,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    valid_from          DATE            NOT NULL,
    valid_until         DATE            NULL,
    version_code        VARCHAR(40)     NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_tax_rules PRIMARY KEY (id),
    CONSTRAINT fk_tr_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT ck_tr_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_tr_kind CHECK (tax_kind IN (
        'ICMS', 'ICMS_ST', 'FCP', 'FCP_ST', 'IPI', 'PIS', 'COFINS', 'DIFAL',
        'IBS', 'CBS', 'IS', 'OTHER'
    ))
);

CREATE INDEX idx_tax_rules_org_code ON tax_rules (organization_id, code);
CREATE INDEX idx_tax_rules_validity ON tax_rules (status, valid_from, valid_until);

CREATE TABLE tax_rule_conditions (
    id                  UUID            NOT NULL,
    rule_id             UUID            NOT NULL,
    field_name          VARCHAR(60)     NOT NULL,
    operator            VARCHAR(20)     NOT NULL,
    value_text          VARCHAR(500)    NULL,
    sort_order          INT             NOT NULL DEFAULT 0,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_tax_rule_conditions PRIMARY KEY (id),
    CONSTRAINT fk_trc_rule FOREIGN KEY (rule_id) REFERENCES tax_rules (id) ON DELETE CASCADE
);

CREATE TABLE tax_rule_results (
    id                  UUID            NOT NULL,
    rule_id             UUID            NOT NULL,
    result_key          VARCHAR(60)     NOT NULL,
    result_value        VARCHAR(200)    NULL,
    numeric_value       NUMERIC(19, 6)  NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_tax_rule_results PRIMARY KEY (id),
    CONSTRAINT fk_trr_rule FOREIGN KEY (rule_id) REFERENCES tax_rules (id) ON DELETE CASCADE
);

CREATE TABLE tax_rule_versions (
    id                  UUID            NOT NULL,
    rule_id             UUID            NOT NULL,
    version_code        VARCHAR(40)     NOT NULL,
    notes               VARCHAR(500)    NULL,
    published_at        TIMESTAMPTZ     NULL,
    published_by        UUID            NULL,
    snapshot_json       TEXT            NULL,
    CONSTRAINT pk_tax_rule_versions PRIMARY KEY (id),
    CONSTRAINT fk_trv_rule FOREIGN KEY (rule_id) REFERENCES tax_rules (id) ON DELETE CASCADE
);

CREATE TABLE tax_calculations (
    id                      UUID            NOT NULL,
    organization_id         UUID            NOT NULL,
    store_id                UUID            NOT NULL,
    establishment_id        UUID            NULL,
    simulation              BOOLEAN         NOT NULL DEFAULT TRUE,
    origin_document_type    VARCHAR(40)     NULL,
    origin_document_id      UUID            NULL,
    operation_code          VARCHAR(40)     NULL,
    issued_on               DATE            NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'COMPLETED',
    total_products          NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    total_tax               NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    currency                VARCHAR(3)      NOT NULL DEFAULT 'BRL',
    trace_summary           TEXT            NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_tax_calculations PRIMARY KEY (id),
    CONSTRAINT fk_tc_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_tc_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_tc_est FOREIGN KEY (establishment_id) REFERENCES fiscal_establishments (id),
    CONSTRAINT ck_tc_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE tax_calculation_items (
    id                      UUID            NOT NULL,
    calculation_id          UUID            NOT NULL,
    line_number             INT             NOT NULL,
    product_id              UUID            NULL,
    ncm                     VARCHAR(10)     NULL,
    cest                    VARCHAR(10)     NULL,
    origin_code             VARCHAR(5)      NULL,
    quantity                NUMERIC(19, 6)  NOT NULL DEFAULT 0,
    unit_price              NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    total_amount            NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    tax_breakdown_json      TEXT            NULL,
    selected_rule_codes     TEXT            NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_tax_calculation_items PRIMARY KEY (id),
    CONSTRAINT fk_tci_calc FOREIGN KEY (calculation_id) REFERENCES tax_calculations (id) ON DELETE CASCADE
);

CREATE TABLE tax_calculation_traces (
    id                      UUID            NOT NULL,
    calculation_id          UUID            NOT NULL,
    item_id                 UUID            NULL,
    step_order              INT             NOT NULL,
    message                 VARCHAR(500)    NOT NULL,
    rule_id                 UUID            NULL,
    detail_json             TEXT            NULL,
    CONSTRAINT pk_tax_calculation_traces PRIMARY KEY (id),
    CONSTRAINT fk_tct_calc FOREIGN KEY (calculation_id) REFERENCES tax_calculations (id) ON DELETE CASCADE,
    CONSTRAINT fk_tct_item FOREIGN KEY (item_id) REFERENCES tax_calculation_items (id) ON DELETE SET NULL,
    CONSTRAINT fk_tct_rule FOREIGN KEY (rule_id) REFERENCES tax_rules (id) ON DELETE SET NULL
);

-- Seed mínimo: ICMS interno CE consumidor final (homologação)
INSERT INTO tax_rules (
    id, organization_id, code, name, description, tax_kind, priority, status, valid_from, version_code,
    active, created_at, updated_at, version
) VALUES (
    'e1000000-0000-4000-8000-000000000001', NULL, 'ICMS-CE-INT-CF',
    'ICMS interno CE consumidor final', 'Regra seed homologação', 'ICMS', 100, 'ACTIVE',
    DATE '2000-01-01', 'HOMOLOG-1', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
);

INSERT INTO tax_rule_conditions (id, rule_id, field_name, operator, value_text, sort_order, active, created_at, updated_at, version)
VALUES
    ('e1000000-0000-4000-8000-000000000011', 'e1000000-0000-4000-8000-000000000001', 'originUf', 'EQ', 'CE', 1, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('e1000000-0000-4000-8000-000000000012', 'e1000000-0000-4000-8000-000000000001', 'destinationUf', 'EQ', 'CE', 2, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('e1000000-0000-4000-8000-000000000013', 'e1000000-0000-4000-8000-000000000001', 'finalConsumer', 'EQ', 'true', 3, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0);

INSERT INTO tax_rule_results (id, rule_id, result_key, result_value, numeric_value, active, created_at, updated_at, version)
VALUES
    ('e1000000-0000-4000-8000-000000000021', 'e1000000-0000-4000-8000-000000000001', 'rate', '0.18', 0.180000, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('e1000000-0000-4000-8000-000000000022', 'e1000000-0000-4000-8000-000000000001', 'cst', '00', NULL, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0);

COMMENT ON TABLE tax_rules IS 'Motor fiscal versionado (Prompt 127) — independente de NF-e/NFC-e';
