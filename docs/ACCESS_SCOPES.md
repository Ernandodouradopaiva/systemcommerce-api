# Escopos de acesso — SystemCommerce (Prompts 156–161)

## Tipos

| Escopo | Rank | Uso |
|--------|------|-----|
| GLOBAL_SYSTEM | 100 | Superadmin / operações globais |
| ORGANIZATION | 80 | Toda a organização |
| STORE_GROUP | 60 | Conjunto de lojas do grupo (`group_store_assignments`) |
| STORE | 40 | Loja específica |
| TEAM_RECORDS | 20 | Registros da equipe (hierarquia/times) |
| OWN_RECORDS | 10 | Registros sob responsabilidade do usuário |

Escopo mais amplo **absorve** o mais restrito na consolidação da mesma permissão.

## Entidades

- `PermissionScope` (catálogo)
- `UserStoreAccess` (já existente)
- `GroupStoreAssignment`
- `UserHierarchyAssignment`
- `Team` / `TeamMember` / `TeamManagerAssignment`
- `OrganizationalPosition` / `OrganizationalHierarchy`

## Autorização

```java
@PreAuthorize("@authorizationService.hasPermission('SALES_ORDER_CREATE')")
@PreAuthorize("@authorizationService.hasStorePermission('SALES_ORDER_CANCEL', #storeId)")
@PreAuthorize("@authorizationService.canAccessResource('SALES_ORDER_READ', 'SALES_ORDER', #orderId)")
```

Fonte da verdade: `EffectivePermissionService` (grupos + vínculos + escopos + cache por `access_version`).

## Hierarquia

Hierarquia **não** concede permissões automaticamente — apenas amplia o conjunto de registros quando a permissão tem `TEAM_RECORDS`.
