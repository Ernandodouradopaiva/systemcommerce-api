# Prompts 122–125 — Estabelecimentos, certificados, catálogos e perfil fiscal de produto

Implementação alinhada a [`FISCAL_ARCHITECTURE.md`](./FISCAL_ARCHITECTURE.md).

## Migrations

| Versão | Conteúdo |
|---|---|
| V252 | `fiscal_establishments`, histórico, séries |
| V253 | `digital_certificates`, assignments, validation/usage logs |
| V254 | `fiscal_tax_catalogs` + versões + seeds homologação |
| V255 | `product_fiscal_profiles` (+ stores, classifications, histories) |
| V256 | Permissões `FISCAL_*` |

## APIs

- `/api/v1/fiscal/establishments`
- `/api/v1/fiscal/certificates`
- `/api/v1/fiscal/tax-catalogs`
- `/api/v1/fiscal/product-profiles`

## Front

Grupo de menu **Fiscal**: estabelecimentos, certificados, catálogos.  
Aba **Fiscal** no detalhe do produto.

## Segurança

Certificado/senha cifrados (`SecretEncryptionService`); resposta de consulta sem segredos; sem endpoint de download.
