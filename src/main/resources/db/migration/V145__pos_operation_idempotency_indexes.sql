-- V145: índices para consulta de operações PDV por Idempotency-Key (resiliência)

CREATE INDEX IF NOT EXISTS idx_sales_last_operation_idempotency_key
    ON sales (last_operation_idempotency_key)
    WHERE last_operation_idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sales_idempotency_key
    ON sales (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payments_idempotency_key
    ON payments (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
