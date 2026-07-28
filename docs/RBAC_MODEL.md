# Modelo RBAC — SystemCommerce

## Definições

| Termo | Definição |
|-------|-----------|
| **Usuário** | Identidade autenticável (`users`); status ACTIVE/INACTIVE/BLOCKED |
| **Grupo** | Pacote nomeado de permissões (`roles` / AccessGroup) |
| **Permissão** | Código técnico imutável de uma ação sobre um recurso (`SALES_ORDER_CANCEL`) |
| **Ação** | Verbo autorizável (`CONSULTAR`, `CANCELAR`, ações específicas) |
| **Módulo** | Agrupador funcional (Vendas, Fiscal, Financeiro…) |
| **Recurso** | Entidade de negócio dentro do módulo (Pedido, Conta a pagar…) |
| **Escopo** | ORGANIZATION / STORE / GLOBAL aplicado a vínculos e grants |
| **Permissão efetiva** | União das ALLOW dos grupos ativos e vínculos vigentes do usuário |
| **Superadministrador** | Grupo SYSTEM com capacidade plena no escopo permitido |
| **Admin organização** | Administra usuários/grupos da própria organização |
| **Admin loja** | Administração limitada às lojas com `UserStoreAccess` |
| **Usuário operacional** | Executa ações do dia a dia sem administrar ACL |

## Regras permanentes

- Autorização oficial **somente na API**.
- Usuário ∈ N grupos; grupo ∈ N permissões; permissão ∈ N grupos.
- Grupo/vínculo/permissão **inativos** não concedem acesso.
- Usuário inativo/bloqueado não autentica.
- Admin não concede permissão que não possui (exceto superadmin).
- Usuário não altera os próprios grupos.
- Menor privilégio; auditoria de mudanças; Flyway only; Testcontainers para integração.
