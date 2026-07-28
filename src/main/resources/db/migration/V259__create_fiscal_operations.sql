-- V259: Operações fiscais / natureza da operação (Prompt 128)
CREATE TABLE fiscal_operations (
    id                              UUID            NOT NULL,
    organization_id                 UUID            NOT NULL,
    code                            VARCHAR(40)     NOT NULL,
    name                            VARCHAR(200)    NOT NULL,
    nature_of_operation             VARCHAR(200)    NULL,
    purpose                         VARCHAR(40)     NULL,
    allowed_models                  VARCHAR(20)     NULL,
    default_cfop                    VARCHAR(10)     NULL,
    generates_finance               BOOLEAN         NOT NULL DEFAULT FALSE,
    moves_stock                     BOOLEAN         NOT NULL DEFAULT FALSE,
    stock_direction                 VARCHAR(20)     NOT NULL DEFAULT 'NONE',
    requires_referenced_document    BOOLEAN         NOT NULL DEFAULT FALSE,
    allows_final_consumer           BOOLEAN         NOT NULL DEFAULT TRUE,
    valid_from                      DATE            NOT NULL,
    valid_until                     DATE            NULL,
    status                          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active                          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by                      UUID            NULL,
    updated_by                      UUID            NULL,
    version                         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_operations PRIMARY KEY (id),
    CONSTRAINT uk_fo_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_fo_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT ck_fo_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_fo_stock_dir CHECK (stock_direction IN ('OUT', 'IN', 'NONE'))
);

CREATE TABLE fiscal_operation_rules (
    id                      UUID            NOT NULL,
    operation_id            UUID            NOT NULL,
    origin_uf               CHAR(2)         NULL,
    dest_uf                 CHAR(2)         NULL,
    taxpayer_indicator      VARCHAR(40)     NULL,
    final_consumer          BOOLEAN         NULL,
    cfop                    VARCHAR(10)     NULL,
    tax_rule_code           VARCHAR(40)     NULL,
    priority                INT             NOT NULL DEFAULT 0,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_operation_rules PRIMARY KEY (id),
    CONSTRAINT fk_for_op FOREIGN KEY (operation_id) REFERENCES fiscal_operations (id) ON DELETE CASCADE
);

CREATE TABLE fiscal_operation_item_rules (
    id                      UUID            NOT NULL,
    operation_id            UUID            NOT NULL,
    ncm_prefix              VARCHAR(10)     NULL,
    product_id              UUID            NULL,
    cfop_override           VARCHAR(10)     NULL,
    tax_rule_code           VARCHAR(40)     NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_operation_item_rules PRIMARY KEY (id),
    CONSTRAINT fk_foir_op FOREIGN KEY (operation_id) REFERENCES fiscal_operations (id) ON DELETE CASCADE
);

CREATE TABLE fiscal_operation_store_assignments (
    id                      UUID            NOT NULL,
    operation_id            UUID            NOT NULL,
    store_id                UUID            NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_by              UUID            NULL,
    updated_by              UUID            NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_fiscal_operation_store_assignments PRIMARY KEY (id),
    CONSTRAINT uk_fosa_op_store UNIQUE (operation_id, store_id),
    CONSTRAINT fk_fosa_op FOREIGN KEY (operation_id) REFERENCES fiscal_operations (id) ON DELETE CASCADE,
    CONSTRAINT fk_fosa_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

-- Seeds para ORG-DEFAULT (id fixo do seed padrão)
INSERT INTO fiscal_operations (
    id, organization_id, code, name, nature_of_operation, purpose, allowed_models, default_cfop,
    generates_finance, moves_stock, stock_direction, requires_referenced_document, allows_final_consumer,
    valid_from, status, active, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), o.id, v.code, v.name, v.nature, v.purpose, v.models, v.cfop,
    v.gen_fin, v.mov_stk, v.stk_dir, v.req_ref, v.allow_cf,
    DATE '2000-01-01', 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0
FROM organizations o
CROSS JOIN (VALUES
    ('VENDA_INTERNA', 'Venda dentro do estado', 'Venda de mercadoria', 'SALE', '55,65', '5102', TRUE, TRUE, 'OUT', FALSE, TRUE),
    ('VENDA_INTERESTADUAL', 'Venda interestadual', 'Venda interestadual', 'SALE', '55', '6102', TRUE, TRUE, 'OUT', FALSE, TRUE),
    ('VENDA_CONSUMIDOR', 'Venda consumidor final', 'Venda a consumidor', 'SALE', '55,65', '5102', TRUE, TRUE, 'OUT', FALSE, TRUE),
    ('VENDA_PDV', 'Venda no PDV', 'Venda PDV NFC-e', 'SALE', '65', '5102', TRUE, TRUE, 'OUT', FALSE, TRUE),
    ('DEVOLUCAO_VENDA', 'Devolução de venda', 'Devolução de venda', 'RETURN', '55,65', '1202', TRUE, TRUE, 'IN', TRUE, TRUE),
    ('DEVOLUCAO_COMPRA', 'Devolução de compra', 'Devolução ao fornecedor', 'RETURN', '55', '5202', TRUE, TRUE, 'OUT', TRUE, FALSE),
    ('TRANSFERENCIA', 'Transferência entre lojas', 'Transferência', 'TRANSFER', '55', '5152', FALSE, TRUE, 'OUT', FALSE, FALSE),
    ('BONIFICACAO', 'Bonificação', 'Bonificação', 'BONUS', '55', '5910', FALSE, TRUE, 'OUT', FALSE, TRUE),
    ('REMESSA_CONSERTO', 'Remessa para conserto', 'Remessa consignação/conserto', 'REMITTANCE', '55', '5915', FALSE, TRUE, 'OUT', FALSE, FALSE),
    ('RETORNO_CONSERTO', 'Retorno de conserto', 'Retorno de conserto', 'RETURN_REMITTANCE', '55', '1915', FALSE, TRUE, 'IN', TRUE, FALSE),
    ('REMESSA_DEMO', 'Remessa para demonstração', 'Remessa demonstração', 'REMITTANCE', '55', '5912', FALSE, TRUE, 'OUT', FALSE, FALSE),
    ('COMPRA', 'Compra', 'Compra de mercadoria', 'PURCHASE', '55', '1102', TRUE, TRUE, 'IN', FALSE, FALSE),
    ('ENTRADA_DEVOLUCAO', 'Entrada de devolução', 'Entrada por devolução', 'RETURN', '55', '1202', TRUE, TRUE, 'IN', TRUE, TRUE),
    ('COMPLEMENTO', 'Complemento', 'NF complementar', 'SALE', '55', '5102', TRUE, FALSE, 'NONE', TRUE, TRUE),
    ('AJUSTE', 'Ajuste', 'NF de ajuste', 'SALE', '55', '5605', FALSE, FALSE, 'NONE', FALSE, FALSE)
) AS v(code, name, nature, purpose, models, cfop, gen_fin, mov_stk, stk_dir, req_ref, allow_cf)
WHERE o.code = 'ORG-DEFAULT'
  AND NOT EXISTS (
      SELECT 1 FROM fiscal_operations fo WHERE fo.organization_id = o.id AND fo.code = v.code
  );

COMMENT ON TABLE fiscal_operations IS 'Natureza/operação fiscal (Prompt 128) — não movimenta estoque diretamente';
