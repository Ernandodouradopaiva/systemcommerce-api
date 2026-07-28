# Plano de homologação fiscal — SystemCommerce

> Prompt 150. Ambiente: **HOMOLOGATION** + `systemcommerce.fiscal.sefaz.stub=false` quando a SEFAZ de homologação estiver disponível; stub apenas para smoke interno.

---

## 1. Pré-requisitos

- [ ] Estabelecimento por loja (CNPJ, IE, UF, CRT)
- [ ] Certificado A1 de **homologação** ativo
- [ ] CSC (NFC-e) configurado quando modelo 65
- [ ] Séries de homologação distintas da produção
- [ ] Schemas XSD da versão de leiaute vigente
- [ ] Backup de XML de teste habilitado ([FISCAL_STORAGE.md](./FISCAL_STORAGE.md))

---

## 2. Matriz de cenários

| # | Cenário | Modelo | Resultado esperado |
|---|---------|--------|--------------------|
| H01 | NF-e normal | 55 | Autorizada + XML + protocolo + DANFE |
| H02 | NFC-e normal | 65 | Autorizada + XML + protocolo |
| H03 | Cliente identificado | 55/65 | Destinatário no XML |
| H04 | Consumidor não identificado | 65 | Permitido quando regra UF/CRT ok |
| H05 | Venda interna (mesma UF) | 55 | CFOP interno |
| H06 | Venda interestadual | 55 | CFOP inter + partilha quando cabível |
| H07 | Destinatário contribuinte | 55 | IE / indIEDest corretos |
| H08 | Não contribuinte | 55/65 | Sem IE quando aplicável |
| H09 | Tributações distintas (itens) | 55 | CST/CSOSN/NCM por item |
| H10 | Múltiplos pagamentos | 55/65 | `pag` coerente com totais |
| H11 | Frete | 55 | `vFrete` / modal |
| H12 | Desconto | 55/65 | `vDesc` sem quebrar BC |
| H13 | Cancelamento | 55/65 | Evento autorizado + status CANCELLED |
| H14 | CC-e | 55 | Sequência cumulativa; **não** em 65 |
| H15 | Inutilização | 55/65 | Protocolo de inutilização |
| H16 | Devolução | 55 | Referência + CFOP devolução |
| H17 | Nota complementar | 55 | Finalidade explícita + referência |
| H18 | Contingência | 55/65 | Ativação controlada + retransmissão |
| H19 | Importação de entrada | — | XML imutável + vínculos |
| H20 | Manifestação | — | Ciência/confirmação com histórico |

---

## 3. Fluxos integrados (obrigatórios)

### Compra
```
XML fornecedor → Entrada fiscal → Pedido → Recebimento → Estoque → Conta a pagar
```
Validar: XML não gera estoque sozinho; AP só após política financeira.

### Venda administrativa
```
Pedido → Faturamento → NF-e → Autorização → (estado AUTHORIZED_PENDING_INTEGRATION)
      → Saída estoque → Conta a receber → AUTHORIZED (integração concluída)
```

### PDV
```
Venda + pagamento → NFC-e → Autorização ou contingência válida → conclusão integrada
```

### Cancelamento
```
Evento fiscal autorizado → cancelamento comercial → estoque inverso → estorno financeiro
```
Estados intermediários seguros: não estornar estoque/financeiro se o evento fiscal **não** estiver autorizado.

---

## 4. Evidências

Para cada cenário: print/DANFE, XML autorizado, protocolo, correlation ID, registro em `fiscal_audit_events`.

Preencher [FISCAL_VALIDATION_REPORT.md](./FISCAL_VALIDATION_REPORT.md) ao final.
