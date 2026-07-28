-- V164: origem STORE_LOCAL em sale_items (preço local da loja)

ALTER TABLE sale_items DROP CONSTRAINT IF EXISTS ck_sale_items_price_source;

ALTER TABLE sale_items
    ADD CONSTRAINT ck_sale_items_price_source CHECK (
        price_source IS NULL OR price_source IN ('CATALOG', 'PRICE_TABLE', 'PROMOTIONAL', 'STORE_LOCAL')
    );

COMMENT ON COLUMN sale_items.price_source IS 'Origem: CATALOG, PRICE_TABLE, PROMOTIONAL, STORE_LOCAL';
