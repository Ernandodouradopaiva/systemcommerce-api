-- V278: Alinha colunas UF fiscais CHAR(2) → VARCHAR(2) para validação Hibernate
ALTER TABLE fiscal_establishments ALTER COLUMN uf TYPE VARCHAR(2);
ALTER TABLE fiscal_tax_catalogs ALTER COLUMN uf TYPE VARCHAR(2);
ALTER TABLE product_fiscal_profiles ALTER COLUMN uf TYPE VARCHAR(2);
ALTER TABLE fiscal_operation_rules ALTER COLUMN origin_uf TYPE VARCHAR(2);
ALTER TABLE fiscal_operation_rules ALTER COLUMN dest_uf TYPE VARCHAR(2);
ALTER TABLE fiscal_endpoint_registry ALTER COLUMN uf TYPE VARCHAR(2);
ALTER TABLE fiscal_event_policies ALTER COLUMN uf TYPE VARCHAR(2);
ALTER TABLE fiscal_contingencies ALTER COLUMN uf TYPE VARCHAR(2);
