-- V143: índices para relatórios PDV + permissões

CREATE INDEX IF NOT EXISTS idx_sales_store_sale_date
    ON sales (store_id, sale_date DESC)
    WHERE store_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sales_pos_channel_status_date
    ON sales (channel, status, sale_date DESC)
    WHERE channel = 'POS';

CREATE INDEX IF NOT EXISTS idx_sales_terminal_sale_date
    ON sales (terminal_id, sale_date DESC)
    WHERE terminal_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sales_cash_session_status
    ON sales (cash_session_id, status)
    WHERE cash_session_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payments_method_status_paid
    ON payments (method, status, paid_at DESC)
    WHERE paid_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_cash_movements_session_type
    ON cash_movements (cash_session_id, type);

CREATE INDEX IF NOT EXISTS idx_cash_sessions_store_opened
    ON cash_sessions (store_id, opened_at DESC);

CREATE INDEX IF NOT EXISTS idx_sale_cancellations_requested_status
    ON sale_cancellations (requested_at DESC, status);

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000052', 'POS_REPORT_READ', 'Consultar relatórios do PDV', 'POS',
     'Acessar relatórios operacionais e analíticos do PDV', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000053', 'POS_REPORT_EXPORT', 'Exportar relatórios do PDV', 'POS',
     'Exportar relatórios do PDV (CSV; PDF preparado)', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000054', 'POS_DASHBOARD_READ', 'Dashboard do PDV', 'POS',
     'Consultar indicadores do dashboard operacional do PDV', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN ('POS_REPORT_READ', 'POS_REPORT_EXPORT', 'POS_DASHBOARD_READ')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SELLER'
  AND p.code IN ('POS_REPORT_READ', 'POS_DASHBOARD_READ')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
