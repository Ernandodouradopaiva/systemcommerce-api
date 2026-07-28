-- V265: Cancelamento e políticas de evento (Prompt 137)
CREATE TABLE fiscal_event_policies (
    id                      UUID            NOT NULL,
    uf                      CHAR(2)         NOT NULL,
    model                   VARCHAR(10)     NOT NULL,
    event_type              VARCHAR(40)     NOT NULL,
    deadline_hours          INT             NOT NULL DEFAULT 24,
    requires_approval       BOOLEAN         NOT NULL DEFAULT FALSE,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_event_policies PRIMARY KEY (id),
    CONSTRAINT uk_fep_uf_model_event UNIQUE (uf, model, event_type)
);

INSERT INTO fiscal_event_policies (id, uf, model, event_type, deadline_hours, requires_approval, active, created_at, updated_at, version)
VALUES
    ('f3000000-0000-4000-8000-000000000001', 'CE', '55', 'CANCELAMENTO', 24, FALSE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('f3000000-0000-4000-8000-000000000002', 'CE', '65', 'CANCELAMENTO', 24, FALSE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('f3000000-0000-4000-8000-000000000003', 'CE', '55', 'INUTILIZACAO', 0, TRUE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0);

CREATE TABLE fiscal_cancellation_requests (
    id                      UUID            NOT NULL,
    document_id             UUID            NOT NULL,
    justification           VARCHAR(500)    NOT NULL,
    status                  VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    protocol_number         VARCHAR(60)     NULL,
    sefaz_cstat             VARCHAR(10)     NULL,
    sefaz_xmotivo           VARCHAR(500)    NULL,
    event_xml_ref           TEXT            NULL,
    transmitted_at          TIMESTAMPTZ     NULL,
    idempotency_key         VARCHAR(100)    NOT NULL,
    requested_by            UUID            NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_cancellation_requests PRIMARY KEY (id),
    CONSTRAINT uk_fcr_idem UNIQUE (idempotency_key),
    CONSTRAINT fk_fcr_doc FOREIGN KEY (document_id) REFERENCES fiscal_documents (id),
    CONSTRAINT ck_fcr_just CHECK (char_length(justification) >= 15),
    CONSTRAINT ck_fcr_status CHECK (status IN (
        'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'QUEUED', 'SENT', 'AUTHORIZED', 'REJECTED', 'ERROR'
    ))
);

CREATE TABLE fiscal_cancellation_authorizations (
    id                      UUID            NOT NULL,
    request_id              UUID            NOT NULL,
    approver_user_id        UUID            NULL,
    decision                VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    decided_at              TIMESTAMPTZ     NULL,
    notes                   VARCHAR(500)    NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_cancellation_authorizations PRIMARY KEY (id),
    CONSTRAINT fk_fca_req FOREIGN KEY (request_id) REFERENCES fiscal_cancellation_requests (id) ON DELETE CASCADE,
    CONSTRAINT ck_fca_decision CHECK (decision IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE fiscal_cancellation_attempts (
    id                      UUID            NOT NULL,
    request_id              UUID            NOT NULL,
    attempt_number          INT             NOT NULL,
    response_cstat          VARCHAR(10)     NULL,
    response_xmotivo        VARCHAR(500)    NULL,
    latency_ms              BIGINT          NULL,
    status                  VARCHAR(30)     NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_cancellation_attempts PRIMARY KEY (id),
    CONSTRAINT fk_fcat_req FOREIGN KEY (request_id) REFERENCES fiscal_cancellation_requests (id) ON DELETE CASCADE
);

COMMENT ON TABLE fiscal_cancellation_requests IS 'Cancelamento por evento (Prompt 137) — documento não é apagado; prazos por UF';
COMMENT ON TABLE fiscal_event_policies IS 'Prazos/aprovação de eventos fiscais configuráveis por UF e modelo';
