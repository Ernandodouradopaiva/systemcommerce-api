-- V139: metadados de suspensão, expiração e bloqueio de edição concorrente

ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS suspend_expires_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS suspended_by_id UUID NULL REFERENCES users (id),
    ADD COLUMN IF NOT EXISTS suspended_terminal_id UUID NULL REFERENCES pos_terminals (id),
    ADD COLUMN IF NOT EXISTS edit_lock_owner_id UUID NULL REFERENCES users (id),
    ADD COLUMN IF NOT EXISTS edit_lock_terminal_id UUID NULL REFERENCES pos_terminals (id),
    ADD COLUMN IF NOT EXISTS edit_lock_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS edit_lock_token VARCHAR(80) NULL;

CREATE INDEX IF NOT EXISTS idx_sales_suspended_status
    ON sales (status, store_id, suspended_at DESC)
    WHERE status = 'SUSPENDED';

CREATE INDEX IF NOT EXISTS idx_sales_suspend_expires
    ON sales (suspend_expires_at)
    WHERE status = 'SUSPENDED' AND suspend_expires_at IS NOT NULL;

-- Backfill origem da suspensão a partir dos vínculos já existentes
UPDATE sales
SET suspended_by_id = seller_id,
    suspended_terminal_id = terminal_id,
    suspend_expires_at = suspended_at + INTERVAL '72 hours'
WHERE status = 'SUSPENDED'
  AND suspended_at IS NOT NULL
  AND (suspended_by_id IS NULL OR suspend_expires_at IS NULL);
