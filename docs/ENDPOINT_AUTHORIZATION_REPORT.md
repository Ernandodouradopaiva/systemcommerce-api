# ENDPOINT_AUTHORIZATION_REPORT.md (Prompt 169)

Ferramenta: `EndpointSecurityChecklistTest`

- Controllers em `access` e `hierarchy` **devem** ter `@PreAuthorize`.
- Relatório global lista controllers sem PreAuthorize (dívida legada, não falha o build).
- Novos endpoints: obrigatório `@PreAuthorize` ou `@authorizationService.*`.

Padrão recomendado:

```java
@PreAuthorize("@authorizationService.hasPermission('CODE')")
@PreAuthorize("@authorizationService.hasStorePermission('CODE', #storeId)")
@PreAuthorize("@authorizationService.canAccessResource('CODE', 'TYPE', #id)")
```
