# Backup e restauração de XML fiscais

A perda dos XMLs pode exigir procedimentos formais de recuperação junto à administração tributária. O SystemCommerce mantém **backup próprio** dos artefatos.

## Backup

1. Copiar diretório `systemcommerce.fiscal.storage.localBasePath` (default `./data/fiscal-xml`)
2. Exportar metadados: tabela `fiscal_stored_artifacts`
3. Incluir no job de backup do PostgreSQL as tabelas fiscais (`fiscal_documents`, eventos, protocolos)
4. Preferir snapshots diários + retenção alinhada à política contábil (mín. 5 anos sugerido)

## Restauração (checklist de teste)

- [ ] Restaurar pasta de XML em ambiente de homologação
- [ ] Conferir `content_sha256` via `POST .../artifacts/{id}/verify`
- [ ] Validar download de um documento autorizado
- [ ] Validar exportação em lote ZIP
- [ ] Confirmar que paths únicos impedem sobrescrita acidental

## Incidentes

Se o storage for perdido sem backup:

1. Registrar incidente e auditoria
2. Tentar reconsulta SEFAZ (distribuição / protocolo) onde aplicável
3. Acionar procedimento formal junto à SEFAZ/RFB conforme orientação do contador
4. Não reemitir documentos já autorizados
