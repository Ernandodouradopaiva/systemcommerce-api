# ACCESS_CONTROL_FINAL_CHECKLIST.md (Prompt 169)

- [ ] Autorização oficial somente na API
- [ ] Frontend apenas UX (guards/menus)
- [ ] Usuário ∈ N grupos; permissões = união
- [ ] Escopos GLOBAL_SYSTEM / ORGANIZATION / STORE / TEAM / OWN
- [ ] Sem dualidade Role × AccessGroup
- [ ] JWT com `av` (+ `sid`/`orgId`); mismatch invalida
- [ ] Bloqueio/inativação revoga sessões
- [ ] Permissões CRITICAL com aprovação / sem autoaprovação
- [ ] Último admin protegido + ADMIN_CONTINGENCY
- [ ] Auditoria ACL sem senha/token
- [ ] Relatórios e AccessReview
- [ ] Menu dinâmico `/me/menus`
- [ ] Migrations V280–V284 aplicadas
- [ ] `mvn clean verify` / `npm run lint|test|build` verdes
- [ ] Documentação de orientação permanente (170)
