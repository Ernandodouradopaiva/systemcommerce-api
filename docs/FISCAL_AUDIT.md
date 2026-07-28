# Auditoria e segurança fiscal (Prompt 147)

## Eventos em `fiscal_audit_events`

Configuração, certificado, cálculo, XML, assinatura, transmissão, consulta, autorização, rejeição, contingência, cancelamento, CC-e, inutilização, manifestação, importação, download, reimpressão, ambiente, série, acesso a XML e tentativas indevidas.

Cada evento: organização, loja, estabelecimento, usuário, documento, ação, data, IP, correlation ID, resultado, código, before/after **sanitizados**.

## Não registrar

- senha do certificado / chave privada / tokens / secrets
- XML integral em logs comuns (apenas length + hint)

## Permissões (matriz)

| Código | Uso |
|--------|-----|
| FISCAL_DOCUMENT_READ / CREATE / TRANSMIT / CANCEL / CORRECT / VOID_NUMBER | Ciclo do documento |
| FISCAL_XML_DOWNLOAD / FISCAL_DANFE_PRINT | Entrega |
| FISCAL_CONFIGURATION_MANAGE / FISCAL_TAX_RULE_MANAGE | Config |
| FISCAL_CONTINGENCY_MANAGE / FISCAL_MONITOR_READ | Operação |
| FISCAL_MANIFESTATION_MANAGE | Destinatário |
| FISCAL_GLOBAL_ACCESS | Cross-loja / auditoria ampla |
| FISCAL_REPORT_READ / FISCAL_STORAGE_MANAGE | Relatórios e storage |

API: `GET /api/v1/fiscal/audit-events`
