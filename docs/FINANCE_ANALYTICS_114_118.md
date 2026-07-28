# Fluxo, fechamento, DRE, dashboard e relatórios (Prompts 114–118)

> Cálculos oficiais no backend. A DRE é **gerencial** — não é demonstração contábil oficial sem integração contábil formal.

## Migrations

| Versão | Conteúdo |
|--------|----------|
| V246 | Períodos, fechamentos, checagens, reaberturas, snapshots de saldo |
| V247 | Layout/linhas/mapeamentos/execuções da DRE gerencial |
| V248 | Cenários de fluxo, auditoria de exportação, índices |
| V249 | Permissões |

## 114 — Fluxo de caixa

**Endpoints:** `GET /api/v1/cash-flow`, `/cash-flow/drill-down`, `/cash-flow/export.csv`, `/cash-flow-scenarios`

**Perspectivas:** realizado, previsto, consolidado; breakdowns por loja/conta/categoria/centro de custo.

**Regras:** transferências internas não entram como receita/despesa consolidada; timezone configurável; cenários com fatores; drill-down e exportação auditada.

## 115 — Fechamento

**Endpoints:** `/api/v1/financial-periods`, `.../close`, `.../reopen`, `/financial-closings/{id}`

**Status:** `OPEN`, `UNDER_REVIEW`, `CLOSED`, `REOPENED`

**Checagens:** parcelas abertas, liquidações pendentes, conciliações, caixas abertos, estornos/transferências em rascunho.

**Guarda:** `FinancialPeriodGuard` bloqueia pagamentos/recebimentos/lançamentos/transferências com data em período `CLOSED`. Reabertura exige motivo e `FINANCIAL_PERIOD_REOPEN`. Fechamento não é apagado.

## 116 — DRE gerencial

**Endpoints:** layouts, mappings, `POST /income-statement/execute`, executions, drill-down, export

**Estrutura:** Receita Bruta → Líquida → Margem Bruta → Resultado Operacional → Resultado Gerencial (fórmulas documentadas nas linhas).

**Bases:** competência e caixa; por loja; comparação de períodos. CMV via categorias mapeadas (custo de estoque formal pendente de integração).

## 117 — Dashboard

**Endpoint:** `GET /api/v1/finance-dashboard` (+ drill-down)

Indicadores de saldo, vencimentos, inadimplência, fluxo 7/15/30/60/90, cartões, conciliações, caixas, tops fornecedor/cliente. Cache TTL 30s no backend.

## 118 — Relatórios

**Endpoint:** `GET /api/v1/finance-reports/{reportType}` (+ export csv/pdf, drill-down)

Tipos: AP/AR, vencimentos, inadimplência, pagamentos/recebimentos, extrato, fluxo, previsão, categorias, CC, fornecedores, clientes, cartões, conciliação, transferências, estornos, adiantamentos, renegociações, DRE, posição por loja.

Exportações auditadas em `finance_report_export_audits`.

## Permissões

`CASH_FLOW_*`, `FINANCIAL_PERIOD_*`, `INCOME_STATEMENT_*`, `FINANCE_DASHBOARD_READ`, `FINANCE_REPORT_*`
