-- V228: Seeds + permissões cadastros financeiros (Prompts 92–95)
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version)
VALUES
    ('a1000000-0000-4000-8000-000000000256', 'FINANCIAL_ACCOUNT_READ', 'Consultar plano de contas', 'FINANCE',
     'Consultar contas e categorias financeiras', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000257', 'FINANCIAL_ACCOUNT_CREATE', 'Criar plano de contas', 'FINANCE',
     'Cadastrar contas e categorias financeiras', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000258', 'FINANCIAL_ACCOUNT_UPDATE', 'Atualizar plano de contas', 'FINANCE',
     'Atualizar e reorganizar contas financeiras', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000259', 'FINANCIAL_ACCOUNT_STATUS_MANAGE', 'Status plano de contas', 'FINANCE',
     'Ativar/inativar contas e categorias', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000260', 'COST_CENTER_READ', 'Consultar centros de custo', 'FINANCE',
     'Consultar centros de custo e vínculos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000261', 'COST_CENTER_CREATE', 'Criar centros de custo', 'FINANCE',
     'Cadastrar centros de custo', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000262', 'COST_CENTER_UPDATE', 'Atualizar centros de custo', 'FINANCE',
     'Atualizar centros e vínculos com loja', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000263', 'COST_CENTER_STATUS_MANAGE', 'Status centros de custo', 'FINANCE',
     'Ativar/inativar centros de custo', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000264', 'BANK_ACCOUNT_READ', 'Consultar contas bancárias', 'FINANCE',
     'Consultar contas bancárias da organização', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000265', 'BANK_ACCOUNT_CREATE', 'Criar contas bancárias', 'FINANCE',
     'Cadastrar contas bancárias', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000266', 'BANK_ACCOUNT_UPDATE', 'Atualizar contas bancárias', 'FINANCE',
     'Atualizar dados cadastrais de contas bancárias', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000267', 'BANK_ACCOUNT_BALANCE_READ', 'Consultar saldo bancário', 'FINANCE',
     'Consultar saldo derivado das movimentações', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000268', 'FINANCIAL_CASH_READ', 'Consultar caixas financeiros', 'FINANCE',
     'Consultar caixas administrativos e PDV', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000269', 'FINANCIAL_CASH_MANAGE', 'Gerenciar caixas financeiros', 'FINANCE',
     'Cadastrar e gerenciar caixas financeiros', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000270', 'BANK_READ', 'Consultar bancos', 'FINANCE',
     'Consultar cadastro de bancos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000271', 'BANK_MANAGE', 'Gerenciar bancos', 'FINANCE',
     'Cadastrar e atualizar bancos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000272', 'PAYMENT_METHOD_READ', 'Consultar formas de pagamento', 'FINANCE',
     'Consultar catálogo de formas de pagamento', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000273', 'PAYMENT_METHOD_MANAGE', 'Gerenciar formas de pagamento', 'FINANCE',
     'Cadastrar formas e configurações por loja', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000274', 'PAYMENT_CONDITION_READ', 'Consultar condições de pagamento', 'FINANCE',
     'Consultar condições e parcelas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
    ('a1000000-0000-4000-8000-000000000275', 'PAYMENT_CONDITION_MANAGE', 'Gerenciar condições de pagamento', 'FINANCE',
     'Cadastrar condições e calcular vencimentos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW() AT TIME ZONE 'UTC'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'MANAGER')
  AND p.code IN (
      'FINANCIAL_ACCOUNT_READ', 'FINANCIAL_ACCOUNT_CREATE', 'FINANCIAL_ACCOUNT_UPDATE', 'FINANCIAL_ACCOUNT_STATUS_MANAGE',
      'COST_CENTER_READ', 'COST_CENTER_CREATE', 'COST_CENTER_UPDATE', 'COST_CENTER_STATUS_MANAGE',
      'BANK_ACCOUNT_READ', 'BANK_ACCOUNT_CREATE', 'BANK_ACCOUNT_UPDATE', 'BANK_ACCOUNT_BALANCE_READ',
      'FINANCIAL_CASH_READ', 'FINANCIAL_CASH_MANAGE', 'BANK_READ', 'BANK_MANAGE',
      'PAYMENT_METHOD_READ', 'PAYMENT_METHOD_MANAGE', 'PAYMENT_CONDITION_READ', 'PAYMENT_CONDITION_MANAGE'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Seeds plano de contas (ORG-DEFAULT)
DO $$
DECLARE
    org_id UUID := 'b1000000-0000-4000-8000-000000000001';
    id_rev UUID := 'c1000000-0000-4000-8000-000000000001';
    id_rev_vend UUID := 'c1000000-0000-4000-8000-000000000002';
    id_rev_serv UUID := 'c1000000-0000-4000-8000-000000000003';
    id_rev_fin UUID := 'c1000000-0000-4000-8000-000000000004';
    id_exp UUID := 'c1000000-0000-4000-8000-000000000010';
    id_exp_comp UUID := 'c1000000-0000-4000-8000-000000000011';
    id_exp_adm UUID := 'c1000000-0000-4000-8000-000000000012';
    id_exp_com UUID := 'c1000000-0000-4000-8000-000000000013';
    id_exp_fin UUID := 'c1000000-0000-4000-8000-000000000014';
    id_exp_imp UUID := 'c1000000-0000-4000-8000-000000000015';
BEGIN
    IF NOT EXISTS (SELECT 1 FROM organizations WHERE id = org_id) THEN
        RETURN;
    END IF;

    INSERT INTO financial_accounts (id, organization_id, code, name, description, parent_id, level_no, account_type, nature, accepts_posting, requires_cost_center, status, sort_order, active, created_at, updated_at, version)
    VALUES
        (id_rev, org_id, '1', 'Receitas', 'Grupo de receitas', NULL, 1, 'REVENUE', 'CREDIT', FALSE, FALSE, 'ACTIVE', 1, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        (id_rev_vend, org_id, '1.1', 'Vendas de Mercadorias', NULL, id_rev, 2, 'REVENUE', 'CREDIT', TRUE, FALSE, 'ACTIVE', 1, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        (id_rev_serv, org_id, '1.2', 'Serviços', NULL, id_rev, 2, 'REVENUE', 'CREDIT', TRUE, FALSE, 'ACTIVE', 2, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        (id_rev_fin, org_id, '1.3', 'Receitas Financeiras', NULL, id_rev, 2, 'REVENUE', 'CREDIT', TRUE, FALSE, 'ACTIVE', 3, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        (id_exp, org_id, '2', 'Despesas', 'Grupo de despesas', NULL, 1, 'EXPENSE', 'DEBIT', FALSE, FALSE, 'ACTIVE', 2, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        (id_exp_comp, org_id, '2.1', 'Compras de Mercadorias', NULL, id_exp, 2, 'EXPENSE', 'DEBIT', TRUE, TRUE, 'ACTIVE', 1, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        (id_exp_adm, org_id, '2.2', 'Despesas Administrativas', NULL, id_exp, 2, 'EXPENSE', 'DEBIT', TRUE, TRUE, 'ACTIVE', 2, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        (id_exp_com, org_id, '2.3', 'Despesas Comerciais', NULL, id_exp, 2, 'EXPENSE', 'DEBIT', TRUE, TRUE, 'ACTIVE', 3, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        (id_exp_fin, org_id, '2.4', 'Despesas Financeiras', NULL, id_exp, 2, 'EXPENSE', 'DEBIT', TRUE, FALSE, 'ACTIVE', 4, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        (id_exp_imp, org_id, '2.5', 'Impostos', NULL, id_exp, 2, 'EXPENSE', 'DEBIT', TRUE, FALSE, 'ACTIVE', 5, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
    ON CONFLICT (id) DO NOTHING;

    -- Closure rows (self + ancestors)
    INSERT INTO financial_account_hierarchy (id, organization_id, ancestor_id, descendant_id, depth)
    SELECT gen_random_uuid(), org_id, a.id, a.id, 0 FROM financial_accounts a WHERE a.organization_id = org_id
    ON CONFLICT DO NOTHING;

    INSERT INTO financial_account_hierarchy (id, organization_id, ancestor_id, descendant_id, depth)
    VALUES
        (gen_random_uuid(), org_id, id_rev, id_rev_vend, 1),
        (gen_random_uuid(), org_id, id_rev, id_rev_serv, 1),
        (gen_random_uuid(), org_id, id_rev, id_rev_fin, 1),
        (gen_random_uuid(), org_id, id_exp, id_exp_comp, 1),
        (gen_random_uuid(), org_id, id_exp, id_exp_adm, 1),
        (gen_random_uuid(), org_id, id_exp, id_exp_com, 1),
        (gen_random_uuid(), org_id, id_exp, id_exp_fin, 1),
        (gen_random_uuid(), org_id, id_exp, id_exp_imp, 1)
    ON CONFLICT DO NOTHING;

    INSERT INTO financial_categories (id, organization_id, code, name, description, financial_account_id, usage_scope, status, active, created_at, updated_at, version)
    VALUES
        ('c1000000-0000-4000-8000-000000000101', org_id, 'CAT-VENDAS', 'Vendas de mercadorias', NULL, id_rev_vend, 'SALE', 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('c1000000-0000-4000-8000-000000000102', org_id, 'CAT-COMPRAS', 'Compras de mercadorias', NULL, id_exp_comp, 'PURCHASE', 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
    ON CONFLICT (id) DO NOTHING;

    -- Centros de custo
    INSERT INTO cost_centers (id, organization_id, code, name, description, parent_id, store_id, accepts_posting, status, sort_order, active, created_at, updated_at, version)
    VALUES
        ('d1000000-0000-4000-8000-000000000001', org_id, 'ADM', 'Administrativo', NULL, NULL, NULL, TRUE, 'ACTIVE', 1, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('d1000000-0000-4000-8000-000000000002', org_id, 'COM', 'Comercial', NULL, NULL, NULL, TRUE, 'ACTIVE', 2, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('d1000000-0000-4000-8000-000000000003', org_id, 'EST', 'Estoque', NULL, NULL, NULL, TRUE, 'ACTIVE', 3, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('d1000000-0000-4000-8000-000000000004', org_id, 'MKT', 'Marketing', NULL, NULL, NULL, TRUE, 'ACTIVE', 4, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('d1000000-0000-4000-8000-000000000005', org_id, 'LOG', 'Logística', NULL, NULL, NULL, TRUE, 'ACTIVE', 5, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('d1000000-0000-4000-8000-000000000006', org_id, 'TEC', 'Tecnologia', NULL, NULL, NULL, TRUE, 'ACTIVE', 6, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('d1000000-0000-4000-8000-000000000007', org_id, 'LJ-CENTRO', 'Loja Centro', NULL, NULL, NULL, TRUE, 'ACTIVE', 7, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('d1000000-0000-4000-8000-000000000008', org_id, 'LJ-SHOP', 'Loja Shopping', NULL, NULL, NULL, TRUE, 'ACTIVE', 8, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO cost_center_hierarchy (id, organization_id, ancestor_id, descendant_id, depth)
    SELECT gen_random_uuid(), org_id, c.id, c.id, 0 FROM cost_centers c WHERE c.organization_id = org_id
    ON CONFLICT DO NOTHING;

    -- Bancos e formas/condições
    INSERT INTO banks (id, organization_id, code, name, short_name, country_code, status, active, created_at, updated_at, version)
    VALUES
        ('e1000000-0000-4000-8000-000000000001', org_id, '001', 'Banco do Brasil S.A.', 'BB', 'BR', 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('e1000000-0000-4000-8000-000000000002', org_id, '341', 'Itaú Unibanco S.A.', 'Itaú', 'BR', 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('e1000000-0000-4000-8000-000000000003', org_id, '237', 'Banco Bradesco S.A.', 'Bradesco', 'BR', 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO fin_payment_methods (id, organization_id, code, name, method_type, allows_purchase, allows_sale, allows_pos, status, sort_order, active, created_at, updated_at, version)
    VALUES
        ('f1000000-0000-4000-8000-000000000001', org_id, 'CASH', 'Dinheiro', 'CASH', TRUE, TRUE, TRUE, 'ACTIVE', 1, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('f1000000-0000-4000-8000-000000000002', org_id, 'PIX', 'PIX', 'PIX', TRUE, TRUE, TRUE, 'ACTIVE', 2, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('f1000000-0000-4000-8000-000000000003', org_id, 'DEBIT', 'Débito', 'DEBIT', TRUE, TRUE, TRUE, 'ACTIVE', 3, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('f1000000-0000-4000-8000-000000000004', org_id, 'CREDIT', 'Crédito', 'CREDIT', TRUE, TRUE, TRUE, 'ACTIVE', 4, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('f1000000-0000-4000-8000-000000000005', org_id, 'BOLETO', 'Boleto', 'BANK_SLIP', TRUE, TRUE, FALSE, 'ACTIVE', 5, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('f1000000-0000-4000-8000-000000000006', org_id, 'TED', 'Transferência', 'TRANSFER', TRUE, TRUE, FALSE, 'ACTIVE', 6, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('f1000000-0000-4000-8000-000000000007', org_id, 'CHEQUE', 'Cheque', 'CHECK', TRUE, TRUE, FALSE, 'ACTIVE', 7, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('f1000000-0000-4000-8000-000000000008', org_id, 'VALE', 'Vale', 'VOUCHER', FALSE, TRUE, TRUE, 'ACTIVE', 8, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('f1000000-0000-4000-8000-000000000009', org_id, 'WALLET', 'Carteira digital', 'DIGITAL_WALLET', TRUE, TRUE, TRUE, 'ACTIVE', 9, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('f1000000-0000-4000-8000-000000000010', org_id, 'CRED-CLI', 'Crédito do cliente', 'CUSTOMER_CREDIT', FALSE, TRUE, TRUE, 'ACTIVE', 10, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('f1000000-0000-4000-8000-000000000011', org_id, 'OUTROS', 'Outros', 'OTHER', TRUE, TRUE, TRUE, 'ACTIVE', 99, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO payment_conditions (id, organization_id, code, name, condition_type, installment_count, interval_days, first_due_days, allows_purchase, allows_sale, allows_pos, status, active, created_at, updated_at, version)
    VALUES
        ('f2000000-0000-4000-8000-000000000001', org_id, 'AVISTA', 'À vista', 'CASH', 1, 0, 0, TRUE, TRUE, TRUE, 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('f2000000-0000-4000-8000-000000000002', org_id, '7D', '7 dias', 'NET_DAYS', 1, 0, 7, TRUE, TRUE, FALSE, 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('f2000000-0000-4000-8000-000000000003', org_id, '15D', '15 dias', 'NET_DAYS', 1, 0, 15, TRUE, TRUE, FALSE, 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('f2000000-0000-4000-8000-000000000004', org_id, '30D', '30 dias', 'NET_DAYS', 1, 0, 30, TRUE, TRUE, FALSE, 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        ('f2000000-0000-4000-8000-000000000005', org_id, '30-60', '30/60 dias', 'INSTALLMENTS', 2, 30, 30, TRUE, TRUE, FALSE, 'ACTIVE', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO payment_condition_installments (id, payment_condition_id, sequence_no, days_offset, percentage, active, created_at, updated_at, version)
    VALUES
        (gen_random_uuid(), 'f2000000-0000-4000-8000-000000000001', 1, 0, 100.0000, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        (gen_random_uuid(), 'f2000000-0000-4000-8000-000000000002', 1, 7, 100.0000, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        (gen_random_uuid(), 'f2000000-0000-4000-8000-000000000003', 1, 15, 100.0000, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        (gen_random_uuid(), 'f2000000-0000-4000-8000-000000000004', 1, 30, 100.0000, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        (gen_random_uuid(), 'f2000000-0000-4000-8000-000000000005', 1, 30, 50.0000, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
        (gen_random_uuid(), 'f2000000-0000-4000-8000-000000000005', 2, 60, 50.0000, TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0)
    ON CONFLICT DO NOTHING;
END $$;
