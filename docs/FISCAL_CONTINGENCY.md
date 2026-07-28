# Contingência fiscal — SystemCommerce

> Complementa [FISCAL_ARCHITECTURE.md](./FISCAL_ARCHITECTURE.md).  
> Pacote: `br.com.systemcommerce.fiscal.contingency`.

---

## 1. Objetivo

Permitir continuidade da operação comercial quando a SEFAZ estiver indisponível ou sob política de contingência da UF, **sem** perder rastreabilidade nem duplicar estoque/financeiro.

A venda comercial / PDV permanece válida conforme regras de negócio; o documento fiscal registra o **tipo de emissão** e segue o fluxo legal até regularização.

---

## 2. Princípios

1. Contingência é **estado controlado** por estabelecimento (não “retry silencioso” infinito).
2. Ativação/desativação auditada; pode exigir permissão `FISCAL_CONTINGENCY_MANAGE` e/ou aprovação.
3. Modos disponíveis dependem da **UF + modelo + NT vigente** (configuráveis).
4. XML e chave gerados em contingência são armazenados como qualquer DFe; transmissão posterior ou eventos de regularização conforme legislação.
5. Numeração continua sob o mesmo controle concorrente de série.
6. Monitor fiscal destaca documentos em contingência pendentes de conclusão.

---

## 3. Modelo

```
FiscalContingencySession
  id, organization_id, store_id, establishment_id
  model (55|65), environment
  mode (enum versionado: OFFLINE | SVC | EPEC | FSDA | … conforme config UF)
  started_at, started_by, ended_at, ended_by
  reason, active
```

Documento fiscal guarda:

- `emission_type` (tpEmis) alinhado ao leiaute da versão;
- `contingency_session_id` (nullable);
- flags de regularização.

---

## 4. Fluxos

### 4.1 Ativação

1. Operador/sistema detecta falha SEFAZ ou política preventiva.
2. Abre `FiscalContingencySession` (se ainda não houver ativa para o escopo).
3. Novas emissões usam `emission_type` de contingência + regras de QR/justificativa.

### 4.2 Emissão em contingência

1. Monta/assina XML com tpEmis adequado.
2. Persistência local imediata (`TRANSMITTED_CONTINGENCY` / status equivalente).
3. Impressão DANFE/DANFE NFC-e permitida conforme regras.
4. Enfileira tentativa de transmissão/autorização quando aplicável ao modo.

### 4.3 Encerramento

1. Sessão encerrada manualmente ou por recuperação do serviço.
2. Documentos pendentes seguem pipeline de autorização/consulta.
3. Divergências → monitor + intervenção humana.

---

## 5. Relação com o PDV

- Checkout **não** é bloqueado apenas por SEFAZ down se a loja permitir contingência ou recibo não fiscal.
- Política por loja: `FAIL_OPEN_RECEIPT | CONTINGENCY_NFCE | BLOCK_CHECKOUT` (config).
- Estoque/pagamento já concluídos não são revertidos pela falha fiscal.

---

## 6. Critérios de aceite

- [x] Sessão de contingência modelada
- [x] Independência do sucesso SEFAZ vs comercial
- [x] Modos por UF/config, não hardcoded exclusivos do CE
- [x] Auditoria de início/fim
- [x] Monitor de pendências previsto
