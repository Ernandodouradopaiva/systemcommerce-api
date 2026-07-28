-- V186: revisão profissional do cadastro de clientes (Prompt 58)
-- Amplia customers com classificação comercial, origem de cadastro, crédito e bloqueio.
-- Colunas flat de endereço/contato permanecem (não removidas) até migração completa para tabelas filhas (V187/V188).

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS classification VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS registration_origin VARCHAR(20) NOT NULL DEFAULT 'ERP',
    ADD COLUMN IF NOT EXISTS commercial_notes VARCHAR(2000) NULL,
    ADD COLUMN IF NOT EXISTS credit_limit NUMERIC(18, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS delinquency_indicator BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS blocked_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS blocked_reason VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS allow_quote_when_blocked BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS municipal_registration VARCHAR(30) NULL;

ALTER TABLE customers
    DROP CONSTRAINT IF EXISTS ck_customers_status;

ALTER TABLE customers
    ADD CONSTRAINT ck_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'));

ALTER TABLE customers
    DROP CONSTRAINT IF EXISTS ck_customers_classification;

ALTER TABLE customers
    ADD CONSTRAINT ck_customers_classification CHECK (
        classification IS NULL OR classification IN ('REGULAR', 'VIP', 'WHOLESALE', 'RESELLER', 'GOVERNMENT', 'OTHER')
    );

ALTER TABLE customers
    DROP CONSTRAINT IF EXISTS ck_customers_registration_origin;

ALTER TABLE customers
    ADD CONSTRAINT ck_customers_registration_origin CHECK (
        registration_origin IN ('ERP', 'POS', 'IMPORT', 'ONLINE', 'OTHER')
    );

ALTER TABLE customers
    DROP CONSTRAINT IF EXISTS ck_customers_credit_limit_non_negative;

ALTER TABLE customers
    ADD CONSTRAINT ck_customers_credit_limit_non_negative CHECK (credit_limit >= 0);

CREATE INDEX IF NOT EXISTS idx_customers_classification ON customers (classification);
CREATE INDEX IF NOT EXISTS idx_customers_blocked_at ON customers (blocked_at);

COMMENT ON COLUMN customers.classification IS 'Classificação comercial: REGULAR|VIP|WHOLESALE|RESELLER|GOVERNMENT|OTHER';
COMMENT ON COLUMN customers.registration_origin IS 'Origem do cadastro: ERP|POS|IMPORT|ONLINE|OTHER';
COMMENT ON COLUMN customers.commercial_notes IS 'Observações comerciais internas (uso da equipe de vendas)';
COMMENT ON COLUMN customers.credit_limit IS 'Limite de crédito — uso futuro (não valida saldo automaticamente ainda)';
COMMENT ON COLUMN customers.delinquency_indicator IS 'Indicador de inadimplência — informativo, alimentado por processo futuro de financeiro';
COMMENT ON COLUMN customers.blocked_at IS 'Data/hora do bloqueio quando status = BLOCKED';
COMMENT ON COLUMN customers.blocked_reason IS 'Motivo do bloqueio comercial';
COMMENT ON COLUMN customers.allow_quote_when_blocked IS 'Quando TRUE, cliente BLOCKED ainda pode ter orçamentos criados (não gera pedido/venda)';
COMMENT ON COLUMN customers.municipal_registration IS 'Inscrição municipal (PJ prestadora de serviço)';
