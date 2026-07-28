# Versionamento normativo e leiautes — SystemCommerce

> Complementa [FISCAL_ARCHITECTURE.md](./FISCAL_ARCHITECTURE.md).  
> Referências externas: Portal Nacional / MOC 7.0 (NF-e e NFC-e), esquemas XSD, Notas Técnicas (incl. **NT 2025.002** e posteriores da Reforma Tributária do Consumo).

---

## 1. Problema a evitar

Espalhar no código:

- URLs de webservice;
- namespaces e versões de schema;
- regras tributárias “mágicas”;
- campos de leiaute da Reforma;

como constantes rígidas acopladas a uma única NT.

---

## 2. Estratégia

```
FiscalLayoutVersion          → versão lógica (ex.: NFe_4.00_NT2025.002)
FiscalSchemaArtifact         → XSD/pacote validável (storage versionado)
FiscalUfServiceConfig        → endpoint + serviço + versão SOAP/REST por UF/ambiente
FiscalTaxRuleSet             → regras de cálculo amarradas à layoutVersion
FiscalDocument.layout_version_id → congelado na emissão
```

**Regra de ouro:** documento histórico permanece na versão em que foi autorizado. Atualizar NT **não** reescreve XML antigo.

---

## 3. Ciclo de atualização normativa

```mermaid
flowchart LR
  A[Publicação NT / XSD] --> B[Ingestão artifact + FiscalLayoutVersion]
  B --> C[Homologação interna + Stub/SEFAZ homolog]
  C --> D[Ativar versão por UF/ambiente]
  D --> E[Novas emissões usam nova versão]
  E --> F[Documentos antigos intactos]
```

Passos operacionais:

1. Adicionar artefatos XSD (Flyway seed ou storage gerenciado) — **sem** apagar XSD anteriores.
2. Criar `FiscalLayoutVersion` com vigência `valid_from` / `valid_to`.
3. Mapear campos novos (Reforma: IBS, CBS, IS, cClassTrib, etc.) em `taxation` com null-safe para versões antigas.
4. Atualizar `FiscalUfServiceConfig` (URLs/serviços) por ambiente.
5. Feature flag / data de corte por UF.
6. Testes de regressão: emitir fixture na versão antiga e na nova.

---

## 4. Reforma Tributária do Consumo

**Implementado (Prompt 145):** tabelas `fiscal_layout_versions`, `fiscal_schema_artifacts`,
`fiscal_tax_rule_set_versions`, `fiscal_rejection_codes`; API `/api/v1/fiscal/layout-versions`;
feature flags IBS/CBS/IS por vigência; seeds `NFe_4.00_BASE` e `NFe_4.00_NT2025.002`.
Campos legados **não** são removidos. Rule sets `locked` impedem alteração retroativa.

Preparação obrigatória na arquitetura (implementação gradual nos prompts seguintes):

| Capacidade | Abordagem |
|---|---|
| Novos grupos no XML | Colunas/JSON versionado em item + serialização por `LayoutSerializer` da versão |
| Regras transitórias | `FiscalTaxRuleSet` por vigência |
| Dupla convivência ICMS×IBS/CBS | Calculadoras plugáveis (`TaxCalculator` por versão) |
| NT 2025.002+ | Novas `FiscalLayoutVersion`; documentos pré-NT intactos |

Serializadores:

```
fiscal.validation + fiscal.document.serialize
  NfeXmlSerializerV400
  NfeXmlSerializerV400_NT2025002
  NfceXmlSerializer…
```

Seleção: `XmlSerializerRegistry.get(layoutVersion)`.

---

## 5. MOC / Portal Nacional

- MOC 7.0 e evoluções: tratados como **documentação de referência** + configuração de serviços, não como import Java direto de PDF.
- Catálogo interno de operações (autorização, retRecepcao, consulta, eventos, inutilização, distribuição) versionado por UF.

---

## 6. Prazos e eventos configuráveis

Tabela conceitual `fiscal_event_policies`:

- UF, modelo, tipo de evento (cancelamento, CCe, …)
- prazo em horas, janela, exige aprovação
- `layout_version_min` aplicável
- atualizável por migration/admin sem alterar histórico

---

## 7. Critérios de aceite

- [x] Leiautes/endpoints/regras versionáveis
- [x] Documentos históricos imutáveis quanto à versão
- [x] Preparação explícita para NT Reforma / 2025.002+
- [x] Atualização por versão, não por “patch” em XML antigo
