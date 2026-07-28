# Segurança de acesso — SystemCommerce

## Controles

| Risco | Mitigação |
|-------|-----------|
| Escalada de privilégio | Admin só concede permissões ⊆ próprias (exceto SUPERADMIN) |
| Autorização só no front | `@PreAuthorize` + revalidação de loja |
| Token com permissões obsoletas | Claim `av` (`access_version`) + refresh resolve do DB |
| Autoatribuição | Bloqueio de alteração dos próprios grupos |
| Último superadmin | Impede remoção/inativação do último vínculo SYSTEM admin |
| Senha | Hash (BCrypt); nunca texto puro |
| Multitenancy | Org/loja em toda operação crítica |

## Auditoria

Tabela `access_change_history` + `DomainAuditService` para:

- criação/edição de grupo;
- grant/revoke de permissão;
- vínculo usuário↔grupo;
- mudanças de status.

## Sessão

1. Login grava `access_version` no JWT.
2. Mudança ACL → `UPDATE users SET access_version = access_version + 1` nos afetados.
3. Filter: se `av` ≠ DB → token rejeitado (401 JWT inválido / sessão desatualizada).
4. Refresh reemite token com permissões e `av` atuais.
