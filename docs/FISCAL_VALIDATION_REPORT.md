# Relatório de validação fiscal — SystemCommerce

> Prompt 150. Preencher ao término da homologação. Status: **EM ABERTO** até cutover.

**Organização:** _________________  
**Período:** ____/____/______ a ____/____/______  
**Responsável técnico:** _________________  
**Contador:** _________________  
**Ambiente:** HOMOLOGATION / PRODUCTION (risco)

---

## 1. Escopo validado

| Bloco | Prompts | Status |
|-------|---------|--------|
| Arquitetura e cadastros | 121–126 | ☐ |
| Tributação / XML / SEFAZ | 127–133 | ☐ |
| NF-e / NFC-e / eventos | 134–141 | ☐ |
| Entrada / DFe / monitor / storage | 142–146 | ☐ |
| Segurança / relatórios / testes / homologação | 147–150 | ☐ |

---

## 2. Resultados por cenário (homologação)

| Cenário | OK | XML | Protocolo | Observação |
|---------|----|-----|-----------|------------|
| H01 NF-e normal | ☐ | ☐ | ☐ | |
| H02 NFC-e normal | ☐ | ☐ | ☐ | |
| H03 Cliente identificado | ☐ | ☐ | ☐ | |
| H04 Consumidor n/i | ☐ | ☐ | ☐ | |
| H05 Interna | ☐ | ☐ | ☐ | |
| H06 Interestadual | ☐ | ☐ | ☐ | |
| H07 Contribuinte | ☐ | ☐ | ☐ | |
| H08 Não contribuinte | ☐ | ☐ | ☐ | |
| H09 Tributações distintas | ☐ | ☐ | ☐ | |
| H10 Múltiplos pagamentos | ☐ | ☐ | ☐ | |
| H11 Frete | ☐ | ☐ | ☐ | |
| H12 Desconto | ☐ | ☐ | ☐ | |
| H13 Cancelamento | ☐ | ☐ | ☐ | |
| H14 CC-e | ☐ | ☐ | ☐ | |
| H15 Inutilização | ☐ | ☐ | ☐ | |
| H16 Devolução | ☐ | ☐ | ☐ | |
| H17 Complementar | ☐ | ☐ | ☐ | |
| H18 Contingência | ☐ | ☐ | ☐ | |
| H19 Entrada XML | ☐ | ☐ | ☐ | |
| H20 Manifestação | ☐ | ☐ | ☐ | |

---

## 3. Validação integrada

| Fluxo | Passou | Evidência |
|-------|--------|-----------|
| Compra (XML→estoque→AP) | ☐ | |
| Venda admin (pedido→NF-e→estoque→AR) | ☐ | |
| PDV (venda→NFC-e→liquidação) | ☐ | |
| Cancelamento (evento→comercial→estoque→financeiro) | ☐ | |
| Falha pós-autorização (PENDING_INTEGRATION) | ☐ | |

---

## 4. Critérios bloqueadores (não-go)

Marcar se **ainda existe**:

| Critério | Existe? |
|----------|---------|
| Documento autorizado sem XML | ☐ |
| XML sem protocolo | ☐ |
| Venda / estoque / financeiro duplicado | ☐ |
| Número fiscal repetido | ☐ |
| Certificado/senha expostos | ☐ |
| Regra fiscal no frontend | ☐ |
| Emissão sem contexto de loja | ☐ |
| IDOR entre lojas | ☐ |
| Contingência sem controle | ☐ |
| Cancelamento fiscal sem reflexo controlado | ☐ |
| Teste falhando (suíte fiscal) | ☐ |
| Migration inválida / schema desatualizado | ☐ |
| Pendência de homologação | ☐ |

**GO apenas se todos = Não.**

---

## 5. Assinaturas

| Papel | Nome | Data | Assinatura |
|-------|------|------|------------|
| TI | | | |
| Contabilidade | | | |
| Operação | | | |
