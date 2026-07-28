# Catálogo de permissões — SystemCommerce

Fonte canônica: tabelas `system_modules`, `system_resources`, `system_actions`, `permissions` (seeds Flyway).

## Módulos

Administração, Cadastros, Produtos, Compras, Estoque, Vendas, PDV, Financeiro, Fiscal, Relatórios, Auditoria, Integrações, Acesso (ACL).

## Ações genéricas

`READ` (CONSULTAR), `CREATE` (INCLUIR), `UPDATE` (EDITAR), `DELETE` (EXCLUIR), `ACTIVATE`, `DISABLE`, `EXPORT`, `PRINT`, `APPROVE`, `CANCEL`, `REVERSE` (ESTORNAR), `AUTHORIZE`, `TRANSMIT`, `REOPEN`, `RECONCILE`.

## Exemplos de códigos imutáveis

| Código | Módulo | Recurso | Ação |
|--------|--------|---------|------|
| USER_READ | Acesso | Usuários | READ |
| ACCESS_GROUP_READ | Acesso | Grupos | READ |
| ACCESS_GROUP_PERMISSION_MANAGE | Acesso | Grupos | AUTHORIZE |
| SALES_ORDER_CANCEL | Vendas | Pedidos | CANCEL |
| PAYABLE_PAY | Financeiro | Contas a pagar | AUTHORIZE |
| FISCAL_DOCUMENT_TRANSMIT | Fiscal | Documentos | TRANSMIT |

## Regras

- Código único e **imutável** após uso.
- Nome/descrição editáveis.
- Não excluir permissão de sistema — inativar.
- Catálogo via migrations; usuário comum não cria permissões livres.
- Endpoint agregado: `GET /api/v1/access-catalog` → Módulo → Recursos → Ações.

Permissões legadas (`permissions.module` string) são vinculadas ao catálogo na migração V280+.
