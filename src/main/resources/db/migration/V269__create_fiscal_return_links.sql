-- V269: Vínculos de devolução fiscal (Prompt 141)
CREATE TABLE fiscal_return_links (
    id                      UUID            NOT NULL,
    return_type             VARCHAR(20)     NOT NULL,
    return_id               UUID            NOT NULL,
    fiscal_document_id      UUID            NULL,
    original_document_id    UUID            NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_return_links PRIMARY KEY (id),
    CONSTRAINT uk_frl_type_return UNIQUE (return_type, return_id),
    CONSTRAINT fk_frl_fiscal_doc FOREIGN KEY (fiscal_document_id) REFERENCES fiscal_documents (id),
    CONSTRAINT fk_frl_orig_doc FOREIGN KEY (original_document_id) REFERENCES fiscal_documents (id),
    CONSTRAINT ck_frl_type CHECK (return_type IN ('SALE', 'PURCHASE')),
    CONSTRAINT ck_frl_status CHECK (status IN ('PENDING', 'AUTHORIZED', 'REJECTED', 'CANCELLED'))
);

COMMENT ON TABLE fiscal_return_links IS 'Devolução fiscal vinculada à comercial (Prompt 141) — estoque fica nos módulos de devolução';
