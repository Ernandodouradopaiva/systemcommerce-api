-- V27: auditoria funcional — módulo, correlation_id, LOGIN_FAILURE e índices de consulta

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS module VARCHAR(40) NULL,
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(100) NULL;

ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS ck_audit_logs_action;
ALTER TABLE audit_logs
    ADD CONSTRAINT ck_audit_logs_action CHECK (action IN (
        'CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE',
        'LOGIN', 'LOGIN_FAILURE', 'LOGOUT', 'STATUS_CHANGE', 'STOCK_MOVEMENT', 'OTHER'
    ));

UPDATE audit_logs SET module = UPPER(entity_name) WHERE module IS NULL;

ALTER TABLE audit_logs ALTER COLUMN module SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_module ON audit_logs (module);
CREATE INDEX IF NOT EXISTS idx_audit_logs_correlation_id ON audit_logs (correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity_performed_at
    ON audit_logs (entity_name, entity_id, performed_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_module_action_performed_at
    ON audit_logs (module, action, performed_at DESC);

COMMENT ON COLUMN audit_logs.module IS 'Módulo funcional (AUTH, USER, CUSTOMER, PRODUCT, INVENTORY, SALE, PAYMENT, ...)';
COMMENT ON COLUMN audit_logs.correlation_id IS 'Correlation ID da requisição HTTP';
