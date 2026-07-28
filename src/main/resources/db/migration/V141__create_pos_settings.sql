-- V141: configurações administrativas do PDV (hierarquia GLOBAL / STORE / TERMINAL)

CREATE TABLE pos_setting_definitions (
    id              UUID         NOT NULL PRIMARY KEY,
    setting_key     VARCHAR(80)  NOT NULL,
    value_type      VARCHAR(20)  NOT NULL,
    category        VARCHAR(40)  NOT NULL,
    label           VARCHAR(150) NOT NULL,
    description     VARCHAR(500) NULL,
    default_value   TEXT         NOT NULL,
    min_value       NUMERIC(19, 4) NULL,
    max_value       NUMERIC(19, 4) NULL,
    allowed_values  TEXT         NULL,
    critical        BOOLEAN      NOT NULL DEFAULT FALSE,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID         NULL,
    updated_by      UUID         NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_pos_setting_definitions_key UNIQUE (setting_key),
    CONSTRAINT ck_pos_setting_definitions_type CHECK (value_type IN ('BOOLEAN', 'INTEGER', 'DECIMAL', 'STRING', 'JSON'))
);

CREATE TABLE pos_settings (
    id              UUID         NOT NULL PRIMARY KEY,
    setting_key     VARCHAR(80)  NOT NULL REFERENCES pos_setting_definitions (setting_key),
    scope           VARCHAR(20)  NOT NULL,
    store_id        UUID         NULL REFERENCES stores (id),
    terminal_id     UUID         NULL REFERENCES pos_terminals (id),
    value_text      TEXT         NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by      UUID         NULL,
    updated_by      UUID         NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_pos_settings_scope CHECK (scope IN ('GLOBAL', 'STORE', 'TERMINAL')),
    CONSTRAINT ck_pos_settings_scope_refs CHECK (
        (scope = 'GLOBAL' AND store_id IS NULL AND terminal_id IS NULL)
        OR (scope = 'STORE' AND store_id IS NOT NULL AND terminal_id IS NULL)
        OR (scope = 'TERMINAL' AND terminal_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_pos_settings_key_scope
    ON pos_settings (setting_key, scope, COALESCE(store_id, '00000000-0000-0000-0000-000000000000'), COALESCE(terminal_id, '00000000-0000-0000-0000-000000000000'))
    WHERE active = TRUE;

CREATE INDEX idx_pos_settings_store ON pos_settings (store_id) WHERE store_id IS NOT NULL;
CREATE INDEX idx_pos_settings_terminal ON pos_settings (terminal_id) WHERE terminal_id IS NOT NULL;
CREATE INDEX idx_pos_settings_key ON pos_settings (setting_key);

CREATE TABLE pos_setting_history (
    id              UUID         NOT NULL PRIMARY KEY,
    setting_id      UUID         NULL REFERENCES pos_settings (id),
    setting_key     VARCHAR(80)  NOT NULL,
    scope           VARCHAR(20)  NOT NULL,
    store_id        UUID         NULL,
    terminal_id     UUID         NULL,
    old_value       TEXT         NULL,
    new_value       TEXT         NULL,
    change_type     VARCHAR(20)  NOT NULL,
    reason          VARCHAR(500) NULL,
    changed_by_id   UUID         NULL REFERENCES users (id),
    changed_at      TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    correlation_id  VARCHAR(64)  NULL,
    CONSTRAINT ck_pos_setting_history_type CHECK (change_type IN ('CREATE', 'UPDATE', 'DELETE', 'VALIDATE'))
);

CREATE INDEX idx_pos_setting_history_key ON pos_setting_history (setting_key, changed_at DESC);
CREATE INDEX idx_pos_setting_history_scope ON pos_setting_history (scope, store_id, terminal_id, changed_at DESC);

-- Catálogo + defaults globais
INSERT INTO pos_setting_definitions (id, setting_key, value_type, category, label, description, default_value, min_value, max_value, allowed_values, critical, sort_order) VALUES
('b1000000-0000-4000-8000-000000000001', 'REQUIRE_CUSTOMER_ON_SALE', 'BOOLEAN', 'SALE', 'Exigir cliente na venda', 'Obriga identificação de cliente antes de finalizar', 'false', NULL, NULL, NULL, FALSE, 10),
('b1000000-0000-4000-8000-000000000002', 'ALLOW_UNIDENTIFIED_CONSUMER', 'BOOLEAN', 'SALE', 'Permitir consumidor não identificado', 'Permite venda sem cliente cadastrado', 'true', NULL, NULL, NULL, FALSE, 20),
('b1000000-0000-4000-8000-000000000003', 'ALLOW_NEGATIVE_STOCK', 'BOOLEAN', 'INVENTORY', 'Permitir estoque negativo', 'Permite confirmar venda com saldo insuficiente', 'false', NULL, NULL, NULL, TRUE, 30),
('b1000000-0000-4000-8000-000000000004', 'DEFAULT_OPERATOR_DISCOUNT_PERCENT', 'DECIMAL', 'DISCOUNT', 'Limite padrão de desconto do operador (%)', 'Teto percentual padrão quando não houver limite por perfil', '10', 0, 100, NULL, FALSE, 40),
('b1000000-0000-4000-8000-000000000005', 'HIGH_WITHDRAWAL_LIMIT', 'DECIMAL', 'CASH', 'Limite de sangria sem autorização', 'Sangria acima deste valor exige autorização', '500.00', 0, NULL, NULL, TRUE, 50),
('b1000000-0000-4000-8000-000000000006', 'MAX_CASH_IN_DRAWER', 'DECIMAL', 'CASH', 'Valor máximo em dinheiro no caixa', 'Alerta quando o saldo físico ultrapassa o limite', '2000.00', 0, NULL, NULL, FALSE, 60),
('b1000000-0000-4000-8000-000000000007', 'AUTO_WITHDRAWAL_ALERT', 'BOOLEAN', 'CASH', 'Solicitar sangria automática por alerta', 'Sugere sangria ao atingir MAX_CASH_IN_DRAWER', 'true', NULL, NULL, NULL, FALSE, 70),
('b1000000-0000-4000-8000-000000000008', 'ALLOW_SUSPENDED_SALE', 'BOOLEAN', 'SUSPENDED', 'Permitir venda suspensa', 'Habilita suspensão de rascunhos no PDV', 'true', NULL, NULL, NULL, FALSE, 80),
('b1000000-0000-4000-8000-000000000009', 'SUSPENDED_SALE_TTL_HOURS', 'INTEGER', 'SUSPENDED', 'Validade da venda suspensa (horas)', 'Após o prazo a suspensa não pode ser recuperada', '72', 1, 720, NULL, FALSE, 90),
('b1000000-0000-4000-8000-000000000010', 'AUTO_PRINT', 'BOOLEAN', 'PRINT', 'Imprimir automaticamente', 'Imprime comprovante após finalização', 'true', NULL, NULL, NULL, FALSE, 100),
('b1000000-0000-4000-8000-000000000011', 'PRINT_COPIES', 'INTEGER', 'PRINT', 'Número de vias', 'Quantidade de vias do comprovante', '1', 1, 5, NULL, FALSE, 110),
('b1000000-0000-4000-8000-000000000012', 'PRINTER_WIDTH', 'INTEGER', 'PRINT', 'Largura da impressora', 'Largura em mm (58 ou 80)', '80', 58, 80, '58,80', FALSE, 120),
('b1000000-0000-4000-8000-000000000013', 'RECEIPT_FOOTER_MESSAGE', 'STRING', 'PRINT', 'Mensagem do comprovante', 'Rodapé impresso no comprovante não fiscal', 'Obrigado pela preferência!', NULL, NULL, NULL, FALSE, 130),
('b1000000-0000-4000-8000-000000000014', 'REQUIRE_ITEM_CANCEL_REASON', 'BOOLEAN', 'CANCEL', 'Exigir motivo para cancelamento de item', 'Motivo obrigatório ao cancelar item', 'true', NULL, NULL, NULL, FALSE, 140),
('b1000000-0000-4000-8000-000000000015', 'REQUIRE_CANCEL_AUTHORIZATION', 'BOOLEAN', 'CANCEL', 'Exigir autorização para cancelamento', 'Cancelamento de item/venda exige autorização', 'false', NULL, NULL, NULL, TRUE, 150),
('b1000000-0000-4000-8000-000000000016', 'ENABLED_PAYMENT_METHODS', 'JSON', 'PAYMENT', 'Formas de pagamento habilitadas', 'Lista JSON de métodos (CASH, PIX, CREDIT_CARD, ...)', '["CASH","PIX","CREDIT_CARD","DEBIT_CARD"]', NULL, NULL, NULL, FALSE, 160),
('b1000000-0000-4000-8000-000000000017', 'MAX_INSTALLMENTS', 'INTEGER', 'PAYMENT', 'Número máximo de parcelas', 'Parcelas máximas no cartão', '12', 1, 48, NULL, FALSE, 170),
('b1000000-0000-4000-8000-000000000018', 'MIN_INSTALLMENT_AMOUNT', 'DECIMAL', 'PAYMENT', 'Valor mínimo por parcela', 'Valor mínimo de cada parcela', '10.00', 0.01, NULL, NULL, FALSE, 180),
('b1000000-0000-4000-8000-000000000019', 'ALLOW_CLOSE_WITH_DIFFERENCE', 'BOOLEAN', 'CASH_CLOSE', 'Permitir fechamento com diferença', 'Permite fechar caixa com diferença de conferência', 'true', NULL, NULL, NULL, TRUE, 190),
('b1000000-0000-4000-8000-000000000020', 'CLOSE_DIFFERENCE_LIMIT', 'DECIMAL', 'CASH_CLOSE', 'Limite de diferença no fechamento', 'Diferença absoluta máxima permitida sem bloqueio', '10.00', 0, NULL, NULL, TRUE, 200),
('b1000000-0000-4000-8000-000000000021', 'REQUIRE_CLOSE_JUSTIFICATION', 'BOOLEAN', 'CASH_CLOSE', 'Exigir justificativa no fechamento', 'Justificativa obrigatória quando houver diferença', 'true', NULL, NULL, NULL, FALSE, 210),
('b1000000-0000-4000-8000-000000000022', 'INACTIVITY_TIMEOUT_MINUTES', 'INTEGER', 'SECURITY', 'Tempo de inatividade (minutos)', 'Minutos sem atividade antes de alertar/bloquear', '15', 1, 240, NULL, FALSE, 220),
('b1000000-0000-4000-8000-000000000023', 'BLOCK_TERMINAL_ON_INACTIVITY', 'BOOLEAN', 'SECURITY', 'Bloquear terminal por inatividade', 'Bloqueia o terminal após timeout', 'false', NULL, NULL, NULL, TRUE, 230),
('b1000000-0000-4000-8000-000000000024', 'SOUNDS_ENABLED', 'BOOLEAN', 'UX', 'Sons do PDV', 'Habilita feedback sonoro padrão do terminal', 'true', NULL, NULL, NULL, FALSE, 240),
('b1000000-0000-4000-8000-000000000025', 'SHORTCUTS_JSON', 'JSON', 'UX', 'Atalhos padrão', 'Mapa JSON de atalhos de teclado padrão do PDV', '{}', NULL, NULL, NULL, FALSE, 250);

-- Valores globais iniciais (= defaults do catálogo)
INSERT INTO pos_settings (id, setting_key, scope, store_id, terminal_id, value_text, active, created_at, updated_at, version)
SELECT
    gen_random_uuid(),
    setting_key,
    'GLOBAL',
    NULL,
    NULL,
    default_value,
    TRUE,
    NOW() AT TIME ZONE 'UTC',
    NOW() AT TIME ZONE 'UTC',
    0
FROM pos_setting_definitions;
