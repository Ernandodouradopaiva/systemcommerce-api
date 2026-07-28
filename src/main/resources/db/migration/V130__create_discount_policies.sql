-- V130: políticas de desconto, limites por perfil e autorizações

CREATE TABLE discount_policies (

    id              UUID            NOT NULL,

    code            VARCHAR(40)     NOT NULL,

    name            VARCHAR(200)    NOT NULL,

    description     VARCHAR(1000)   NULL,

    applies_to      VARCHAR(20)     NOT NULL,

    product_id      UUID            NULL,

    category_id     UUID            NULL,

    max_percent     NUMERIC(7, 4)   NOT NULL,

    max_amount      NUMERIC(19, 2)  NULL,

    priority        INTEGER         NOT NULL DEFAULT 0,

    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',

    valid_from      TIMESTAMPTZ     NULL,

    valid_to        TIMESTAMPTZ     NULL,

    active          BOOLEAN         NOT NULL DEFAULT TRUE,

    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),

    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),

    created_by      UUID            NULL,

    updated_by      UUID            NULL,

    version         BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_discount_policies PRIMARY KEY (id),

    CONSTRAINT uk_discount_policies_code UNIQUE (code),

    CONSTRAINT fk_discount_policies_product FOREIGN KEY (product_id) REFERENCES products (id),

    CONSTRAINT fk_discount_policies_category FOREIGN KEY (category_id) REFERENCES categories (id),

    CONSTRAINT ck_discount_policies_applies CHECK (applies_to IN ('GLOBAL', 'PRODUCT', 'CATEGORY')),

    CONSTRAINT ck_discount_policies_status CHECK (status IN ('ACTIVE', 'INACTIVE')),

    CONSTRAINT ck_discount_policies_percent CHECK (max_percent >= 0 AND max_percent <= 100),

    CONSTRAINT ck_discount_policies_scope CHECK (

        (applies_to = 'GLOBAL' AND product_id IS NULL AND category_id IS NULL)

        OR (applies_to = 'PRODUCT' AND product_id IS NOT NULL AND category_id IS NULL)

        OR (applies_to = 'CATEGORY' AND category_id IS NOT NULL AND product_id IS NULL)

    ),

    CONSTRAINT ck_discount_policies_period CHECK (valid_to IS NULL OR valid_from IS NULL OR valid_to >= valid_from)

);



CREATE TABLE operator_discount_limits (

    id              UUID            NOT NULL,

    role_id         UUID            NOT NULL,

    max_percent     NUMERIC(7, 4)   NOT NULL,

    max_amount      NUMERIC(19, 2)  NULL,

    active          BOOLEAN         NOT NULL DEFAULT TRUE,

    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),

    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),

    created_by      UUID            NULL,

    updated_by      UUID            NULL,

    version         BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_operator_discount_limits PRIMARY KEY (id),

    CONSTRAINT uk_operator_discount_limits_role UNIQUE (role_id),

    CONSTRAINT fk_odl_role FOREIGN KEY (role_id) REFERENCES roles (id),

    CONSTRAINT ck_odl_percent CHECK (max_percent >= 0 AND max_percent <= 100)

);



CREATE TABLE discount_authorizations (

    id                      UUID            NOT NULL,

    sale_id                 UUID            NOT NULL,

    sale_item_id            UUID            NULL,

    requested_amount        NUMERIC(19, 2)  NOT NULL,

    requested_percent       NUMERIC(7, 4)   NULL,

    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',

    request_reason          VARCHAR(500)    NULL,

    decision_notes          VARCHAR(500)    NULL,

    requested_by_id         UUID            NOT NULL,

    decided_by_id           UUID            NULL,

    decided_at              TIMESTAMPTZ     NULL,

    active                  BOOLEAN         NOT NULL DEFAULT TRUE,

    created_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),

    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),

    created_by              UUID            NULL,

    updated_by              UUID            NULL,

    version                 BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_discount_authorizations PRIMARY KEY (id),

    CONSTRAINT fk_da_sale FOREIGN KEY (sale_id) REFERENCES sales (id),

    CONSTRAINT fk_da_item FOREIGN KEY (sale_item_id) REFERENCES sale_items (id),

    CONSTRAINT fk_da_requested_by FOREIGN KEY (requested_by_id) REFERENCES users (id),

    CONSTRAINT fk_da_decided_by FOREIGN KEY (decided_by_id) REFERENCES users (id),

    CONSTRAINT ck_da_status CHECK (status IN ('PENDING', 'APPROVED', 'DENIED')),

    CONSTRAINT ck_da_amount CHECK (requested_amount >= 0)

);



CREATE INDEX idx_discount_policies_applies ON discount_policies (applies_to, status);

CREATE INDEX idx_discount_authorizations_sale ON discount_authorizations (sale_id, status);

CREATE INDEX idx_discount_authorizations_status ON discount_authorizations (status);



COMMENT ON TABLE discount_policies IS 'Políticas de desconto por global/produto/categoria';

COMMENT ON TABLE operator_discount_limits IS 'Limite máximo de desconto por perfil (role)';

COMMENT ON TABLE discount_authorizations IS 'Solicitação/aprovação de desconto acima do limite';


