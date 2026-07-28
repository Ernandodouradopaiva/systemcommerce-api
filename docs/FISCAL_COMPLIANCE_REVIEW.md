# Revisão de conformidade fiscal — SystemCommerce

> Prompt 150. Revisão formal antes de produção.

---

## 1. Separação de responsabilidades

| Elemento | Responsabilidade | Histórico próprio |
|----------|------------------|-------------------|
| Documento comercial | Negociação (pedido, venda, compra) | Sim |
| Estoque | Quantidades | Sim |
| Financeiro | Valores / títulos | Sim |
| Documento fiscal | Obrigação perante a administração tributária | Sim |

Devem estar **vinculados** (`origin_document_type/id`, loja, organização) sem fundir responsabilidades.

---

## 2. Conformidade operacional

| Tema | Diretriz SystemCommerce |
|------|-------------------------|
| NFC-e | Não recebe CC-e |
| Contingência | Só por indisponibilidade/comunicação — não por rejeição |
| Autorização SEFAZ | Não prova adequação material; alertas/validações permanecem |
| XML autorizado | Imutável; backup próprio obrigatório |
| Reforma Tributária | Campos legados preservados; flags por vigência (NT 2025.002+) |
| Manifestação | Histórico de eventos + último estado |
| Entrada | XML fornecedor imutável; estoque só via recebimento |

---

## 3. Controles de não-conformidade (bloqueadores)

Conforme critério final do Prompt 150 — módulo **não** concluído se existir:

1. Autorizado sem XML  
2. XML sem protocolo  
3. Duplicidade comercial / estoque / financeiro  
4. Numeração fiscal repetida  
5. Certificado ou senha expostos  
6. Regra fiscal calculada no frontend  
7. Emissão sem contexto de loja  
8. Acesso cross-loja (IDOR)  
9. Contingência pendente sem controle  
10. Cancelamento fiscal sem reflexo controlado  
11. Teste falhando / migration inválida / schema desatualizado  
12. Pendência de homologação  

---

## 4. Ordem de implantação recomendada

| Faixa | Escopo |
|-------|--------|
| 121–126 | Arquitetura, estabelecimento, cadastros |
| 127–133 | Tributação, XML, assinatura, SEFAZ |
| 134–141 | NF-e, NFC-e, eventos |
| 142–146 | Entrada, distribuição, monitor, storage |
| 147–150 | Segurança, relatórios, testes, homologação |

---

## 5. Parecer

| Campo | Conteúdo |
|-------|----------|
| Parecer contábil | ☐ Conforme ☐ Com ressalvas ☐ Não conforme |
| Ressalvas | |
| Parecer TI | ☐ Conforme ☐ Com ressalvas ☐ Não conforme |
| Data | |
| Assinaturas | |
