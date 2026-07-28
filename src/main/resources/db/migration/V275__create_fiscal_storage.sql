-- V275: Armazenamento seguro de XML / artefatos fiscais (Prompt 146)

CREATE TABLE fiscal_retention_policies (
    id                          UUID            NOT NULL,
    organization_id             UUID            NOT NULL,
    model                       VARCHAR(10)     NULL,
    retention_years             INT             NOT NULL DEFAULT 5,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_retention_policies PRIMARY KEY (id),
    CONSTRAINT uk_frp_org_model UNIQUE (organization_id, model),
    CONSTRAINT fk_frp_org FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE TABLE fiscal_stored_artifacts (
    id                          UUID            NOT NULL,
    organization_id             UUID            NOT NULL,
    establishment_id            UUID            NOT NULL,
    document_id                 UUID            NULL,
    artifact_type               VARCHAR(40)     NOT NULL,
    storage_backend             VARCHAR(20)     NOT NULL DEFAULT 'LOCAL',
    storage_path                VARCHAR(500)    NOT NULL,
    content_sha256              VARCHAR(64)     NOT NULL,
    size_bytes                  BIGINT          NULL,
    content_type                VARCHAR(100)    NULL,
    encrypted                   BOOLEAN         NOT NULL DEFAULT FALSE,
    immutable                   BOOLEAN         NOT NULL DEFAULT FALSE,
    metadata_json               TEXT            NULL,
    retention_until             DATE            NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_stored_artifacts PRIMARY KEY (id),
    CONSTRAINT uk_fsa_storage_path UNIQUE (storage_path),
    CONSTRAINT fk_fsa_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_fsa_est FOREIGN KEY (establishment_id) REFERENCES fiscal_establishments (id),
    CONSTRAINT fk_fsa_doc FOREIGN KEY (document_id) REFERENCES fiscal_documents (id),
    CONSTRAINT ck_fsa_type CHECK (artifact_type IN (
        'GENERATED_XML', 'SIGNED_XML', 'SENT_XML', 'RETURN_XML', 'AUTHORIZED_XML',
        'EVENT_XML', 'PROTOCOL', 'DANFE_PDF', 'OTHER')),
    CONSTRAINT ck_fsa_backend CHECK (storage_backend IN ('LOCAL', 'S3', 'DB'))
);

CREATE INDEX idx_fsa_org_est_type ON fiscal_stored_artifacts (organization_id, establishment_id, artifact_type);
CREATE INDEX idx_fsa_document ON fiscal_stored_artifacts (document_id);
