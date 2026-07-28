-- V172: contexto multiloja adicional na trilha de auditoria

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS organization_id UUID NULL REFERENCES organizations (id),
    ADD COLUMN IF NOT EXISTS warehouse_id UUID NULL REFERENCES warehouses (id),
    ADD COLUMN IF NOT EXISTS seller_profile_id UUID NULL REFERENCES seller_profiles (id),
    ADD COLUMN IF NOT EXISTS employee_id UUID NULL REFERENCES employees (id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_organization_performed
    ON audit_logs (organization_id, performed_at DESC)
    WHERE organization_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_warehouse_performed
    ON audit_logs (warehouse_id, performed_at DESC)
    WHERE warehouse_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_seller_profile_performed
    ON audit_logs (seller_profile_id, performed_at DESC)
    WHERE seller_profile_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_employee_performed
    ON audit_logs (employee_id, performed_at DESC)
    WHERE employee_id IS NOT NULL;

COMMENT ON COLUMN audit_logs.organization_id IS 'Organização do contexto operacional';
COMMENT ON COLUMN audit_logs.warehouse_id IS 'Depósito do contexto operacional';
COMMENT ON COLUMN audit_logs.seller_profile_id IS 'Perfil de vendedor associado ao evento';
COMMENT ON COLUMN audit_logs.employee_id IS 'Funcionário associado ao evento';
