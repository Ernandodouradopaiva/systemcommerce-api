# Checklist final do módulo fiscal — SystemCommerce

> Prompt 150. Usar junto com [FISCAL_VALIDATION_REPORT.md](./FISCAL_VALIDATION_REPORT.md).

## Integração (regra definitiva)

- [ ] Venda administrativa: Pedido → Faturamento → NF-e → Autorização → estoque/financeiro
- [ ] PDV: Venda/pagamento → NFC-e → Autorização ou contingência válida → conclusão
- [ ] Compra: DFe fornecedor → conferência → recebimento → estoque → AP
- [ ] Quatro eixos vinculados e com histórico próprio: **comercial / estoque / financeiro / fiscal**
- [ ] Estados intermediários seguros após autorização SEFAZ (`AUTHORIZED_PENDING_INTEGRATION`)

## Dados e migração

- [ ] Inventário de vendas, faturamentos, pagamentos, clientes, fornecedores, produtos, lojas
- [ ] Config tributária e séries alinhadas
- [ ] Histórico externo importado **sem** emissão retroativa automática
- [ ] Numeração = max(legado)+1

## Homologação

- [ ] Matriz H01–H20 executada ([FISCAL_HOMOLOGATION_PLAN.md](./FISCAL_HOMOLOGATION_PLAN.md))
- [ ] Fluxos compra / venda / PDV / cancelamento evidenciados

## Segurança e compliance

- [ ] Certificado nunca em texto puro / logs
- [ ] XML download auditado; sem XML integral em logs comuns
- [ ] Permissões granulares; sem IDOR entre lojas
- [ ] [FISCAL_COMPLIANCE_REVIEW.md](./FISCAL_COMPLIANCE_REVIEW.md) assinado

## Operação

- [ ] Monitor + dead-letter ativos
- [ ] Contingência com runbook
- [ ] Backup/restore de XML testado
- [ ] Dashboard/relatórios com exportação auditada

## Engenharia

- [ ] Migrations Flyway aplicadas sem drift
- [ ] Suíte fiscal unitária verde
- [ ] Schemas XSD da versão implantada documentados
- [ ] Feature flags reforma (IBS/CBS/IS) por vigência

## Produção

- [ ] [FISCAL_PRODUCTION_READINESS.md](./FISCAL_PRODUCTION_READINESS.md) = GO
