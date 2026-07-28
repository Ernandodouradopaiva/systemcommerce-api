-- V263: Schemas XML versionados (Prompt 131)
CREATE TABLE fiscal_schema_versions (
    id                      UUID            NOT NULL,
    model                   VARCHAR(10)     NOT NULL,
    layout_version          VARCHAR(40)     NOT NULL,
    schema_namespace        VARCHAR(200)    NULL,
    xsd_resource_path       VARCHAR(500)    NULL,
    xsd_content             TEXT            NULL,
    valid_from              DATE            NULL,
    valid_until             DATE            NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_schema_versions PRIMARY KEY (id),
    CONSTRAINT uk_fsv_model_layout UNIQUE (model, layout_version),
    CONSTRAINT ck_fsv_status CHECK (status IN ('DRAFT', 'ACTIVE', 'DEPRECATED')),
    CONSTRAINT ck_fsv_model CHECK (model IN ('55', '65'))
);

CREATE TABLE fiscal_schema_update_histories (
    id                      UUID            NOT NULL,
    schema_version_id       UUID            NOT NULL,
    imported_at             TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    imported_by             UUID            NULL,
    notes                   VARCHAR(2000)   NULL,
    source                  VARCHAR(200)    NULL,
    CONSTRAINT pk_fiscal_schema_update_histories PRIMARY KEY (id),
    CONSTRAINT fk_fsuh_schema FOREIGN KEY (schema_version_id) REFERENCES fiscal_schema_versions (id) ON DELETE CASCADE
);

INSERT INTO fiscal_schema_versions (
    id, model, layout_version, schema_namespace, xsd_resource_path, valid_from, status,
    active, created_at, updated_at, version
) VALUES
    ('f1000000-0000-4000-8000-000000000001', '55', '4.00', 'http://www.portalfiscal.inf.br/nfe',
     'classpath:fiscal/xsd/nfe_v4.00.xsd', DATE '2000-01-01', 'ACTIVE',
     TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('f1000000-0000-4000-8000-000000000002', '65', '4.00', 'http://www.portalfiscal.inf.br/nfe',
     'classpath:fiscal/xsd/nfce_v4.00.xsd', DATE '2000-01-01', 'ACTIVE',
     TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0);

COMMENT ON TABLE fiscal_schema_versions IS 'Leiautes/XSD versionados (Prompt 131) — documentos históricos não mudam';
