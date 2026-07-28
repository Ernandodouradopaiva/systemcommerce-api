# Prompts 130–137 — Numeração, XML, assinatura, SEFAZ, emissão, DANFE, cancelamento

| Prompt | Pacote | Migration |
|---|---|---|
| 130 | `fiscal.numbering` | V262 |
| 131 | `fiscal.validation` / XML | V263 |
| 132 | `fiscal.certificate.signing` | — |
| 133 | `fiscal.transmission` | V264 |
| 134 | `fiscal.nfe` | — |
| 135 | `fiscal.nfce` + hook PDV | — |
| 136 | `fiscal.print` | — |
| 137 | `fiscal.event` | V265 |
| Perms | | V266 |

## Destaques

- Homologação/produção com sequências separadas; inutilização ≠ cancelamento.
- XML via XMLStreamWriter; schemas versionados; docs históricos intactos.
- `FiscalSignatureProvider`: A1 + `TestSignatureProvider` (sem cert real em testes).
- SEFAZ por adapter/stub (`systemcommerce.fiscal.sefaz.stub=true`); registry por UF.
- NF-e/NFC-e sem rebaixar estoque nem regenerar AR; PDV soft-fail após finance.
- DANFE a partir do XML autorizado/contingência; cancelamento por evento com prazo por UF.
