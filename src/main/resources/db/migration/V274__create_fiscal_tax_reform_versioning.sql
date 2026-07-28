-- V274: Reforma tributária / versionamento fiscal (Prompt 145)

CREATE TABLE fiscal_layout_versions (
    id                          UUID            NOT NULL,
    code                        VARCHAR(60)     NOT NULL,
    description                 VARCHAR(255)    NULL,
    model                       VARCHAR(10)     NOT NULL DEFAULT 'ALL',
    schema_namespace            VARCHAR(200)    NULL,
    valid_from                  DATE            NOT NULL,
    valid_to                    DATE            NULL,
    feature_flags_json          TEXT            NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_layout_versions PRIMARY KEY (id),
    CONSTRAINT uk_flv_code UNIQUE (code)
);

CREATE TABLE fiscal_schema_artifacts (
    id                          UUID            NOT NULL,
    layout_version_id           UUID            NOT NULL,
    artifact_name               VARCHAR(120)    NOT NULL,
    version_label               VARCHAR(60)     NOT NULL,
    content_sha256              VARCHAR(64)     NULL,
    storage_path                VARCHAR(500)    NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_schema_artifacts PRIMARY KEY (id),
    CONSTRAINT uk_fsa_layout_name_ver UNIQUE (layout_version_id, artifact_name, version_label),
    CONSTRAINT fk_fsa_layout FOREIGN KEY (layout_version_id) REFERENCES fiscal_layout_versions (id)
);

CREATE TABLE fiscal_tax_rule_set_versions (
    id                          UUID            NOT NULL,
    code                        VARCHAR(60)     NOT NULL,
    layout_version_id           UUID            NOT NULL,
    valid_from                  DATE            NOT NULL,
    valid_to                    DATE            NULL,
    rules_json                  TEXT            NULL,
    locked                      BOOLEAN         NOT NULL DEFAULT FALSE,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_tax_rule_set_versions PRIMARY KEY (id),
    CONSTRAINT uk_ftrsv_code UNIQUE (code),
    CONSTRAINT fk_ftrsv_layout FOREIGN KEY (layout_version_id) REFERENCES fiscal_layout_versions (id)
);

CREATE TABLE fiscal_rejection_codes (
    id                          UUID            NOT NULL,
    code                        VARCHAR(10)     NOT NULL,
    model                       VARCHAR(10)     NOT NULL DEFAULT 'ALL',
    layout_version_id           UUID            NULL,
    message                     VARCHAR(500)    NOT NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_rejection_codes PRIMARY KEY (id),
    CONSTRAINT uk_frc_code_model_layout UNIQUE (code, model, layout_version_id),
    CONSTRAINT fk_frc_layout FOREIGN KEY (layout_version_id) REFERENCES fiscal_layout_versions (id)
);

ALTER TABLE fiscal_documents
    ADD COLUMN IF NOT EXISTS layout_version_id UUID NULL,
    ADD COLUMN IF NOT EXISTS tax_reform_payload_json TEXT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_fd_layout_version'
    ) THEN
        ALTER TABLE fiscal_documents
            ADD CONSTRAINT fk_fd_layout_version
            FOREIGN KEY (layout_version_id) REFERENCES fiscal_layout_versions (id);
    END IF;
END $$;

INSERT INTO fiscal_layout_versions (id, code, description, model, schema_namespace, valid_from, valid_to, feature_flags_json, active, created_at, updated_at, version)
VALUES
    ('b1000000-0000-4000-8000-000000000001',
     'NFe_4.00_BASE',
     'NF-e/NFC-e 4.00 vigente sem flags reforma',
     'ALL',
     'http://www.portalfiscal.inf.br/nfe',
     DATE '2020-01-01',
     DATE '2025-12-31',
     '{"ibs":false,"cbs":false,"is":false,"nt":"base"}',
     TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('b1000000-0000-4000-8000-000000000002',
     'NFe_4.00_NT2025.002',
     'Adequações Reforma Tributária — NT 2025.002 (preparação / convivência)',
     'ALL',
     'http://www.portalfiscal.inf.br/nfe',
     DATE '2026-01-01',
     NULL,
     '{"ibs":true,"cbs":true,"is":true,"nt":"2025.002"}',
     TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;
