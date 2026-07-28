-- V100: seed de permissões iniciais
-- UUIDs fixos para referências estáveis entre ambientes

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at, version) VALUES
( 'a1000000-0000-4000-8000-000000000001', 'USER_READ', 'Listar usuários', 'USER', 'Consulta de usuários', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000002', 'USER_CREATE', 'Criar usuário', 'USER', 'Cadastro de usuários', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000003', 'USER_UPDATE', 'Atualizar usuário', 'USER', 'Edição de usuários', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000004', 'USER_DELETE', 'Inativar usuário', 'USER', 'Inativação de usuários', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000005', 'ROLE_READ', 'Listar perfis', 'ROLE', 'Consulta de perfis', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000006', 'ROLE_MANAGE', 'Gerenciar perfis', 'ROLE', 'Manutenção de perfis e permissões', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000007', 'CUSTOMER_READ', 'Listar clientes', 'CUSTOMER', 'Consulta de clientes', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000008', 'CUSTOMER_CREATE', 'Criar cliente', 'CUSTOMER', 'Cadastro de clientes', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000009', 'CUSTOMER_UPDATE', 'Atualizar cliente', 'CUSTOMER', 'Edição de clientes', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-00000000000a', 'CUSTOMER_DELETE', 'Inativar cliente', 'CUSTOMER', 'Inativação de clientes', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-00000000000b', 'CATEGORY_READ', 'Listar categorias', 'CATEGORY', 'Consulta de categorias', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-00000000000c', 'CATEGORY_MANAGE', 'Gerenciar categorias', 'CATEGORY', 'CRUD de categorias', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-00000000000d', 'PRODUCT_READ', 'Listar produtos', 'PRODUCT', 'Consulta de produtos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-00000000000e', 'PRODUCT_CREATE', 'Criar produto', 'PRODUCT', 'Cadastro de produtos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-00000000000f', 'PRODUCT_UPDATE', 'Atualizar produto', 'PRODUCT', 'Edição de produtos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000010', 'PRODUCT_DELETE', 'Inativar produto', 'PRODUCT', 'Inativação de produtos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000011', 'INVENTORY_READ', 'Consultar estoque', 'INVENTORY', 'Consulta de saldos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000012', 'INVENTORY_MOVE', 'Movimentar estoque', 'INVENTORY', 'Entradas, saídas e ajustes', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000013', 'SALE_READ', 'Listar vendas', 'SALE', 'Consulta de vendas', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000014', 'SALE_CREATE', 'Criar venda', 'SALE', 'Abertura e itens de venda', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000015', 'SALE_CONFIRM', 'Confirmar venda', 'SALE', 'Confirmação com baixa de estoque', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000016', 'SALE_CANCEL', 'Cancelar venda', 'SALE', 'Cancelamento com estorno', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000017', 'PAYMENT_MANAGE', 'Gerenciar pagamentos', 'PAYMENT', 'Registro e consulta de pagamentos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000018', 'DASHBOARD_READ', 'Visualizar dashboard', 'DASHBOARD', 'Indicadores agregados', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0),
( 'a1000000-0000-4000-8000-000000000019', 'REPORT_READ', 'Visualizar relatórios', 'REPORT', 'Relatórios básicos', TRUE, NOW() AT TIME ZONE 'UTC', NOW() AT TIME ZONE 'UTC', 0);
