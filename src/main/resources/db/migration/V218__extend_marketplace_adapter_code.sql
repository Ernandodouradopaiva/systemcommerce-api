-- V218: Extensões de canal para adapters (Prompts 83–85)
-- Adapters Mercado Livre / Shopee / WooCommerce usam settings_json e credentials_encrypted
-- do hub (V215). Esta migration registra metadados de sincronização por tipo de adapter.

ALTER TABLE marketplace_accounts
    ADD COLUMN IF NOT EXISTS adapter_code VARCHAR(40) NULL;

COMMENT ON COLUMN marketplace_accounts.adapter_code IS
    'Código do adapter: MERCADO_LIVRE | SHOPEE | WOOCOMMERCE | GENERIC (Prompts 83–85)';

CREATE INDEX IF NOT EXISTS idx_ma_adapter ON marketplace_accounts (adapter_code)
    WHERE adapter_code IS NOT NULL;
