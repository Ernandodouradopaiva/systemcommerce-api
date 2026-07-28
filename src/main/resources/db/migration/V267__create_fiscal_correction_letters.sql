-- V267: Carta de Correção Eletrônica — CC-e (Prompt 138)
CREATE TABLE fiscal_correction_letter_sequences (
    id                  UUID            NOT NULL,
    document_id         UUID            NOT NULL,
    next_sequence       INT             NOT NULL DEFAULT 1,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_cce_sequences PRIMARY KEY (id),
    CONSTRAINT uk_fcls_document UNIQUE (document_id),
    CONSTRAINT fk_fcls_doc FOREIGN KEY (document_id) REFERENCES fiscal_documents (id)
);

CREATE TABLE fiscal_correction_letters (
    id                      UUID            NOT NULL,
    document_id             UUID            NOT NULL,
    sequence                INT             NOT NULL,
    correction_text         TEXT            NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    protocol_number         VARCHAR(60)     NULL,
    sefaz_cstat             VARCHAR(10)     NULL,
    sefaz_xmotivo           VARCHAR(500)    NULL,
    transmitted_at          TIMESTAMPTZ     NULL,
    idempotency_key         VARCHAR(100)    NOT NULL,
    requested_by            UUID            NULL,
    validation_warnings     TEXT            NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_correction_letters PRIMARY KEY (id),
    CONSTRAINT uk_fcl_doc_seq UNIQUE (document_id, sequence),
    CONSTRAINT uk_fcl_idem UNIQUE (idempotency_key),
    CONSTRAINT fk_fcl_doc FOREIGN KEY (document_id) REFERENCES fiscal_documents (id),
    CONSTRAINT ck_fcl_status CHECK (status IN ('DRAFT', 'QUEUED', 'SENT', 'AUTHORIZED', 'REJECTED', 'ERROR')),
    CONSTRAINT ck_fcl_text CHECK (char_length(correction_text) >= 15)
);

CREATE TABLE fiscal_correction_letter_event_xmls (
    id                  UUID            NOT NULL,
    letter_id           UUID            NOT NULL,
    kind                VARCHAR(40)     NOT NULL,
    content             TEXT            NULL,
    sha256              VARCHAR(64)     NULL,
    stored_at           TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_fiscal_cce_event_xmls PRIMARY KEY (id),
    CONSTRAINT fk_fcex_letter FOREIGN KEY (letter_id) REFERENCES fiscal_correction_letters (id) ON DELETE CASCADE
);

INSERT INTO fiscal_event_policies (id, uf, model, event_type, deadline_hours, requires_approval, active, created_at, updated_at, version)
VALUES
    ('f3000000-0000-4000-8000-000000000010', 'CE', '55', 'CCE', 720, FALSE, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (uf, model, event_type) DO NOTHING;

COMMENT ON TABLE fiscal_correction_letters IS 'CC-e apenas NF-e 55 (Prompt 138); NFC-e não aplicável; original imutável';
