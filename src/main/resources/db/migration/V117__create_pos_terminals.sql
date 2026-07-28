-- V117: terminais de PDV
CREATE TABLE pos_terminals (
    id                      UUID            NOT NULL,
    store_id                UUID            NOT NULL,
    warehouse_id            UUID            NOT NULL,
    code                    VARCHAR(40)     NOT NULL,
    name                    VARCHAR(200)    NOT NULL,
    terminal_number         INTEGER         NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    station_identifier      VARCHAR(100)    NULL,
    printer_name            VARCHAR(200)    NULL,
    print_model             VARCHAR(40)     NOT NULL DEFAULT 'NONE',
    last_communication_at   TIMESTAMPTZ     NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_pos_terminals PRIMARY KEY (id),
    CONSTRAINT uk_pos_terminals_store_code UNIQUE (store_id, code),
    CONSTRAINT uk_pos_terminals_store_number UNIQUE (store_id, terminal_number),
    CONSTRAINT fk_pos_terminals_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_pos_terminals_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT ck_pos_terminals_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_pos_terminals_code_not_blank CHECK (LENGTH(TRIM(code)) > 0),
    CONSTRAINT ck_pos_terminals_number_positive CHECK (terminal_number > 0),
    CONSTRAINT ck_pos_terminals_print_model CHECK (
        print_model IN ('NONE', 'THERMAL_58', 'THERMAL_80', 'A4')
    )
);

CREATE INDEX idx_pos_terminals_store_id ON pos_terminals (store_id);
CREATE INDEX idx_pos_terminals_warehouse_id ON pos_terminals (warehouse_id);
CREATE INDEX idx_pos_terminals_status ON pos_terminals (status);
CREATE INDEX idx_pos_terminals_station ON pos_terminals (station_identifier)
    WHERE station_identifier IS NOT NULL;

COMMENT ON TABLE pos_terminals IS 'Terminais de PDV vinculados a loja e depósito';
COMMENT ON COLUMN pos_terminals.terminal_number IS 'Número do terminal único dentro da loja';
COMMENT ON COLUMN pos_terminals.station_identifier IS 'Identificador técnico da estação (hostname/device id)';
-- Sessão de caixa aberta única por terminal será enforced em cash_sessions (prompt seguinte)
-- via UNIQUE parcial: UNIQUE (terminal_id) WHERE status = 'OPEN'
