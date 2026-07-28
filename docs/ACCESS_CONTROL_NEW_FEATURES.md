# Orientação permanente — novas funcionalidades (Prompt 170)

Toda nova funcionalidade no SystemCommerce **deve**:

1. Identificar módulo → recurso → ações  
2. Criar códigos técnicos imutáveis (`RESOURCE_ACTION`)  
3. Seed via Flyway (nunca só pelo front)  
4. Vincular ao catálogo (module/resource/action)  
5. Proteger endpoints (`@PreAuthorize` / `@authorizationService`)  
6. Aplicar escopo (org/loja/equipe/próprio)  
7. Integrar grupos e menu (`/me/menus` + nav)  
8. Proteger botões/rotas (`PermissionGuard`, `PermissionButton`)  
9. Auditar mudanças sensíveis  
10. Testar (unit/integration)  
11. Atualizar `PERMISSION_CATALOG.md` e `PERMISSION_COVERAGE_REPORT.md`

## Proibido

- Endpoint sem permissão  
- Confiar só no frontend  
- Usar label como código  
- Conceder permissão direto no browser  
- Aceitar org/loja do client sem validar  
- Duplicar permissões  
- Excluir fisicamente permissão de sistema  
- Manter acesso removido até o token expirar (use `av`)  
- Acesso admin sem auditoria  
- Regras ad hoc por controller  

## Modelo definitivo

```
Usuário → N:N Grupos → N:N Permissões → Módulo+Recurso+Ação → Escopo
```

**O grupo determina o que.** **O escopo determina onde e sobre quais dados.**
