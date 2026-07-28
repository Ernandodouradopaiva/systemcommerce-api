-- V131: snapshot de preço/autorização no item da venda

ALTER TABLE sale_items

    ADD COLUMN IF NOT EXISTS price_source VARCHAR(20) NULL,

    ADD COLUMN IF NOT EXISTS price_table_id UUID NULL,

    ADD COLUMN IF NOT EXISTS product_price_id UUID NULL,

    ADD COLUMN IF NOT EXISTS discount_authorized_by_id UUID NULL;



UPDATE sale_items

SET price_source = 'CATALOG'

WHERE price_source IS NULL;



ALTER TABLE sale_items

    ALTER COLUMN price_source SET DEFAULT 'CATALOG';



ALTER TABLE sale_items DROP CONSTRAINT IF EXISTS ck_sale_items_price_source;

ALTER TABLE sale_items

    ADD CONSTRAINT ck_sale_items_price_source CHECK (

        price_source IS NULL OR price_source IN ('CATALOG', 'PRICE_TABLE', 'PROMOTIONAL')

    );



ALTER TABLE sale_items DROP CONSTRAINT IF EXISTS fk_sale_items_price_table;

ALTER TABLE sale_items

    ADD CONSTRAINT fk_sale_items_price_table FOREIGN KEY (price_table_id) REFERENCES price_tables (id);



ALTER TABLE sale_items DROP CONSTRAINT IF EXISTS fk_sale_items_product_price;

ALTER TABLE sale_items

    ADD CONSTRAINT fk_sale_items_product_price FOREIGN KEY (product_price_id) REFERENCES product_prices (id);



ALTER TABLE sale_items DROP CONSTRAINT IF EXISTS fk_sale_items_discount_auth_by;

ALTER TABLE sale_items

    ADD CONSTRAINT fk_sale_items_discount_auth_by FOREIGN KEY (discount_authorized_by_id) REFERENCES users (id);



ALTER TABLE sales

    ADD COLUMN IF NOT EXISTS discount_authorized_by_id UUID NULL;



ALTER TABLE sales DROP CONSTRAINT IF EXISTS fk_sales_discount_auth_by;

ALTER TABLE sales

    ADD CONSTRAINT fk_sales_discount_auth_by FOREIGN KEY (discount_authorized_by_id) REFERENCES users (id);



COMMENT ON COLUMN sale_items.price_source IS 'Origem do preço gravado no item (imutável após confirmação)';

COMMENT ON COLUMN sale_items.product_price_id IS 'Registro de product_prices usado; alteração futura no cadastro não afeta a venda';

COMMENT ON COLUMN sale_items.unit_price IS 'Preço unitário oficial aplicado no momento da inclusão (snapshot)';


