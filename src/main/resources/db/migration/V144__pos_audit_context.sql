-- V144: auditoria específica do PDV (contexto + event_code + outcome)

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS event_code VARCHAR(80) NULL,
    ADD COLUMN IF NOT EXISTS outcome VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS error_code VARCHAR(80) NULL,
    ADD COLUMN IF NOT EXISTS store_id UUID NULL REFERENCES stores (id),
    ADD COLUMN IF NOT EXISTS terminal_id UUID NULL REFERENCES pos_terminals (id),
    ADD COLUMN IF NOT EXISTS cash_session_id UUID NULL REFERENCES cash_sessions (id),
    ADD COLUMN IF NOT EXISTS sale_id UUID NULL REFERENCES sales (id),
    ADD COLUMN IF NOT EXISTS operator_id UUID NULL REFERENCES users (id),
    ADD COLUMN IF NOT EXISTS authorized_by_id UUID NULL REFERENCES users (id);

ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS ck_audit_logs_outcome;
ALTER TABLE audit_logs
    ADD CONSTRAINT ck_audit_logs_outcome CHECK (
        outcome IS NULL OR outcome IN ('SUCCESS', 'DENIED', 'FAILED', 'ATTEMPT')
    );

CREATE INDEX IF NOT EXISTS idx_audit_logs_event_code_performed
    ON audit_logs (event_code, performed_at DESC)
    WHERE event_code IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_pos_module_performed
    ON audit_logs (module, performed_at DESC)
    WHERE module = 'POS';

CREATE INDEX IF NOT EXISTS idx_audit_logs_store_performed
    ON audit_logs (store_id, performed_at DESC)
    WHERE store_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_terminal_performed
    ON audit_logs (terminal_id, performed_at DESC)
    WHERE terminal_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_session_performed
    ON audit_logs (cash_session_id, performed_at DESC)
    WHERE cash_session_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_sale_performed
    ON audit_logs (sale_id, performed_at DESC)
    WHERE sale_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_outcome_performed
    ON audit_logs (outcome, performed_at DESC)
    WHERE outcome IS NOT NULL;

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000055', 'POS_AUDIT_READ', 'Consultar auditoria do PDV', 'POS',
     'Consultar trilha de auditoria específica do PDV com filtros operacionais', TRUE,
     NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code = 'POS_AUDIT_READ'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
