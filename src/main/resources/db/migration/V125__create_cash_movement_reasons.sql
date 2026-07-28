-- V125: motivos de suprimento / sangria
CREATE TABLE cash_movement_reasons (
    id                  UUID            NOT NULL,
    code                VARCHAR(40)     NOT NULL,
    description         VARCHAR(200)    NOT NULL,
    applies_to          VARCHAR(20)     NOT NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by          UUID            NULL,
    updated_by          UUID            NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_cash_movement_reasons PRIMARY KEY (id),
    CONSTRAINT uk_cash_movement_reasons_code UNIQUE (code),
    CONSTRAINT ck_cash_movement_reasons_applies CHECK (applies_to IN ('SUPPLY', 'WITHDRAWAL', 'BOTH'))
);

CREATE INDEX idx_cash_movement_reasons_active ON cash_movement_reasons (active);
CREATE INDEX idx_cash_movement_reasons_applies ON cash_movement_reasons (applies_to);

ALTER TABLE cash_movements
    ADD CONSTRAINT fk_cash_movements_reason FOREIGN KEY (reason_id) REFERENCES cash_movement_reasons (id);

COMMENT ON TABLE cash_movement_reasons IS 'Motivos cadastrados para suprimento e sangria de caixa';
