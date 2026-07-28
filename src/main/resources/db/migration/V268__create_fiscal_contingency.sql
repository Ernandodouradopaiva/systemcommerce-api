-- V268: Contingência fiscal (Prompt 139)
CREATE TABLE fiscal_contingencies (
    id                      UUID            NOT NULL,
    establishment_id        UUID            NOT NULL,
    model                   VARCHAR(10)     NOT NULL,
    environment             VARCHAR(20)     NOT NULL,
    mode                    VARCHAR(30)     NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    reason                  VARCHAR(500)    NULL,
    started_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    started_by              UUID            NULL,
    ended_at                TIMESTAMPTZ     NULL,
    ended_by                UUID            NULL,
    uf                      CHAR(2)         NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_contingencies PRIMARY KEY (id),
    CONSTRAINT fk_fc_est FOREIGN KEY (establishment_id) REFERENCES fiscal_establishments (id),
    CONSTRAINT ck_fc_mode CHECK (mode IN ('SVC', 'OFFLINE_NFCE', 'EPEC', 'FS_DA', 'OTHER')),
    CONSTRAINT ck_fc_status CHECK (status IN ('ACTIVE', 'CLOSED')),
    CONSTRAINT ck_fc_env CHECK (environment IN ('HOMOLOGATION', 'PRODUCTION'))
);

CREATE INDEX idx_fc_est_status ON fiscal_contingencies (establishment_id, status);

CREATE TABLE fiscal_contingency_activations (
    id                      UUID            NOT NULL,
    contingency_id          UUID            NOT NULL,
    trigger_kind            VARCHAR(30)     NOT NULL,
    detail_json             TEXT            NULL,
    activated_at            TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_contingency_activations PRIMARY KEY (id),
    CONSTRAINT fk_fca_cont FOREIGN KEY (contingency_id) REFERENCES fiscal_contingencies (id) ON DELETE CASCADE,
    CONSTRAINT ck_fca_trigger CHECK (trigger_kind IN ('NETWORK', 'SERVICE_DOWN', 'MANUAL'))
);

CREATE TABLE fiscal_contingency_documents (
    id                          UUID            NOT NULL,
    contingency_id              UUID            NOT NULL,
    document_id                 UUID            NOT NULL,
    pending_retransmission      BOOLEAN         NOT NULL DEFAULT TRUE,
    last_consult_at             TIMESTAMPTZ     NULL,
    status                      VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_contingency_documents PRIMARY KEY (id),
    CONSTRAINT uk_fcd_cont_doc UNIQUE (contingency_id, document_id),
    CONSTRAINT fk_fcd_cont FOREIGN KEY (contingency_id) REFERENCES fiscal_contingencies (id) ON DELETE CASCADE,
    CONSTRAINT fk_fcd_doc FOREIGN KEY (document_id) REFERENCES fiscal_documents (id),
    CONSTRAINT ck_fcd_status CHECK (status IN ('PENDING', 'RETRANSMITTED', 'AUTHORIZED', 'DISCARDED'))
);

CREATE TABLE fiscal_contingency_transmission_attempts (
    id                          UUID            NOT NULL,
    contingency_document_id     UUID            NOT NULL,
    attempt_number              INT             NOT NULL,
    result                      VARCHAR(40)     NULL,
    cstat                       VARCHAR(10)     NULL,
    xmotivo                     VARCHAR(500)    NULL,
    latency_ms                  BIGINT          NULL,
    active                      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                  UUID            NULL,
    updated_by                  UUID            NULL,
    version                     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_contingency_tx_attempts PRIMARY KEY (id),
    CONSTRAINT fk_fcta_cd FOREIGN KEY (contingency_document_id) REFERENCES fiscal_contingency_documents (id) ON DELETE CASCADE
);

COMMENT ON TABLE fiscal_contingencies IS 'Contingência por modelo/UF (Prompt 139) — não ativar por rejeição fiscal';
