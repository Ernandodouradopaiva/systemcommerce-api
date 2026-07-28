-- V20: ampliação do cadastro de clientes (Prompt 8)
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS trade_name VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS state_registration VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS mobile VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS birth_date DATE NULL,
    ADD COLUMN IF NOT EXISTS notes VARCHAR(2000) NULL,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE customers
SET status = CASE WHEN active THEN 'ACTIVE' ELSE 'INACTIVE' END
WHERE status IS NULL OR status = '';

ALTER TABLE customers
    DROP CONSTRAINT IF EXISTS ck_customers_status;

ALTER TABLE customers
    ADD CONSTRAINT ck_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE'));

-- Garante alinhamento active ↔ status
UPDATE customers SET active = TRUE WHERE status = 'ACTIVE';
UPDATE customers SET active = FALSE WHERE status = 'INACTIVE';

CREATE INDEX IF NOT EXISTS idx_customers_status ON customers (status);
CREATE INDEX IF NOT EXISTS idx_customers_trade_name ON customers (trade_name);
CREATE INDEX IF NOT EXISTS idx_customers_mobile ON customers (mobile);

COMMENT ON COLUMN customers.trade_name IS 'Nome fantasia (PJ) ou nome social alternativo';
COMMENT ON COLUMN customers.state_registration IS 'Inscrição estadual';
COMMENT ON COLUMN customers.mobile IS 'Celular';
COMMENT ON COLUMN customers.birth_date IS 'Data de nascimento (PF)';
COMMENT ON COLUMN customers.notes IS 'Observações livres';
COMMENT ON COLUMN customers.status IS 'ACTIVE | INACTIVE — situação comercial do cliente';
