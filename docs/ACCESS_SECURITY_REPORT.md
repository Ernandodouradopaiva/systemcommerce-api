# ACCESS_SECURITY_REPORT.md (Prompt 169)

| Risco | Controle |
|-------|----------|
| Escalada | Admin ⊆ próprias permissões; CRITICAL via aprovação dual |
| Autoaprovação | Bloqueada |
| Token obsoleto | claim `av` + `sid` |
| Remoção crítica | bump `access_version` imediato |
| Último admin | proteção + `ADMIN_CONTINGENCY` |
| Cross-store/org | StoreAuthorizationEvaluator + ResourceAccessResolver |
| Autoalteração de grupos | `assertNotSelfGroupChange` |
| Segredos em audit | sanitização de password/token/secret |
| Menu ≠ auth | API valida sempre |
