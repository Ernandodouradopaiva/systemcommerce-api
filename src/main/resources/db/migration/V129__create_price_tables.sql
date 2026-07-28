-- V129: tabelas de preços do PDV

CREATE TABLE price_tables (

    id              UUID            NOT NULL,

    code            VARCHAR(40)     NOT NULL,

    name            VARCHAR(200)    NOT NULL,

    description     VARCHAR(1000)   NULL,

    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',

    priority        INTEGER         NOT NULL DEFAULT 0,

    valid_from      TIMESTAMPTZ     NULL,

    valid_to        TIMESTAMPTZ     NULL,

    active          BOOLEAN         NOT NULL DEFAULT TRUE,

    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),

    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),

    created_by      UUID            NULL,

    updated_by      UUID            NULL,

    version         BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_price_tables PRIMARY KEY (id),

    CONSTRAINT uk_price_tables_code UNIQUE (code),

    CONSTRAINT ck_price_tables_status CHECK (status IN ('ACTIVE', 'INACTIVE')),

    CONSTRAINT ck_price_tables_period CHECK (valid_to IS NULL OR valid_from IS NULL OR valid_to >= valid_from)

);



CREATE TABLE price_table_stores (

    price_table_id  UUID            NOT NULL,

    store_id        UUID            NOT NULL,

    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),

    CONSTRAINT pk_price_table_stores PRIMARY KEY (price_table_id, store_id),

    CONSTRAINT fk_pts_table FOREIGN KEY (price_table_id) REFERENCES price_tables (id) ON DELETE CASCADE,

    CONSTRAINT fk_pts_store FOREIGN KEY (store_id) REFERENCES stores (id)

);



CREATE TABLE product_prices (

    id              UUID            NOT NULL,

    price_table_id  UUID            NOT NULL,

    product_id      UUID            NOT NULL,

    price_type      VARCHAR(20)     NOT NULL DEFAULT 'STANDARD',

    unit_price      NUMERIC(19, 2)  NOT NULL,

    min_quantity    NUMERIC(19, 3)  NOT NULL DEFAULT 0,

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

    CONSTRAINT pk_product_prices PRIMARY KEY (id),

    CONSTRAINT fk_product_prices_table FOREIGN KEY (price_table_id) REFERENCES price_tables (id),

    CONSTRAINT fk_product_prices_product FOREIGN KEY (product_id) REFERENCES products (id),

    CONSTRAINT ck_product_prices_type CHECK (price_type IN ('STANDARD', 'PROMOTIONAL')),

    CONSTRAINT ck_product_prices_status CHECK (status IN ('ACTIVE', 'INACTIVE')),

    CONSTRAINT ck_product_prices_price CHECK (unit_price >= 0),

    CONSTRAINT ck_product_prices_min_qty CHECK (min_quantity >= 0),

    CONSTRAINT ck_product_prices_period CHECK (valid_to IS NULL OR valid_from IS NULL OR valid_to >= valid_from)

);



CREATE INDEX idx_price_tables_status ON price_tables (status);

CREATE INDEX idx_price_tables_priority ON price_tables (priority);

CREATE INDEX idx_pts_store ON price_table_stores (store_id);

CREATE INDEX idx_product_prices_lookup ON product_prices (product_id, status, priority);

CREATE INDEX idx_product_prices_table ON product_prices (price_table_id);



COMMENT ON TABLE price_tables IS 'Tabelas de preço do PDV (padrão, loja, período, promoção)';

COMMENT ON TABLE product_prices IS 'Preço de produto em uma tabela; snapshot gravado no item da venda';

COMMENT ON COLUMN product_prices.priority IS 'Maior prioridade vence; conflito com mesma prioridade+período é bloqueado';


