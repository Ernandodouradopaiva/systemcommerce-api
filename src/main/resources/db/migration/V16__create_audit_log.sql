-- V16: auditoria complementar (trilha de ações sensíveis)
CREATE TABLE audit_logs (
    id                  UUID            NOT NULL,
    entity_name         VARCHAR(100)    NOT NULL,
    entity_id           UUID            NULL,
    action              VARCHAR(40)     NOT NULL,
    old_values          JSONB           NULL,
    new_values          JSONB           NULL,
    details             VARCHAR(1000)   NULL,
    ip_address          VARCHAR(45)     NULL,
    performed_by        UUID            NULL,
    performed_at        TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (performed_by) REFERENCES users (id),
    CONSTRAINT ck_audit_logs_action CHECK (action IN (
        'CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE',
        'LOGIN', 'LOGOUT', 'STATUS_CHANGE', 'STOCK_MOVEMENT', 'OTHER'
    )),
    CONSTRAINT ck_audit_logs_entity_name_not_blank CHECK (LENGTH(TRIM(entity_name)) > 0)
);

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_name, entity_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_performed_at ON audit_logs (performed_at);
CREATE INDEX idx_audit_logs_performed_by ON audit_logs (performed_by);

COMMENT ON TABLE audit_logs IS 'Auditoria complementar de ações sensíveis além das colunas de auditoria das entidades';
COMMENT ON COLUMN audit_logs.old_values IS 'Snapshot JSON do estado anterior';
COMMENT ON COLUMN audit_logs.new_values IS 'Snapshot JSON do novo estado';
