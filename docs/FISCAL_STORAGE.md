# Armazenamento fiscal de XML e artefatos

Complementa a arquitetura fiscal. Documentos autorizados são **imutáveis** em disco e no metadado.

## Backends

| Backend | Uso |
|---------|-----|
| `LOCAL` | Padrão (`systemcommerce.fiscal.storage.localBasePath`) |
| `S3` | Compatível (interface pronta; ativação futura) |
| `DB` | Apenas metadados/pequenos — não depender só do banco para XML grandes |

## Organização de paths

`{orgId}/{establishmentId}/{yyyy}/{MM}/{model}/{documentId}/{type}-{sha16}.xml`

## Regras

- Sobrescrita **impedida** (`CREATE_NEW` + unique `storage_path`)
- Autorizado → `immutable=true`
- Download e exportação em lote são **auditados** (sem XML integral nos logs)
- Política de retenção por organização/modelo (`fiscal_retention_policies`)
- Hash SHA-256 para verificação de integridade

## API

- `GET /api/v1/fiscal/storage/documents/{id}/artifacts`
- `GET /api/v1/fiscal/storage/artifacts/{id}/download`
- `POST /api/v1/fiscal/storage/export-batch`
- `POST /api/v1/fiscal/storage/artifacts/{id}/verify`

Ver também [FISCAL_BACKUP_RESTORE.md](./FISCAL_BACKUP_RESTORE.md).
