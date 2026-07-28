# Prontidão para produção fiscal — SystemCommerce

> Prompt 150. Decisão **GO / NO-GO**.

---

## 1. Gate técnico

| Item | GO se… |
|------|--------|
| Certificado A1 produção | Instalado, testado, validade > 30 dias, acesso restrito |
| Ambiente | `PRODUCTION` só após cutover; stub SEFAZ **desligado** |
| Numeração | Sequências alinhadas; sem colisão com legado |
| Storage XML | Backend LOCAL/S3 com backup; restore testado |
| Monitor | Fila + dead-letter operacionais; plantão definido |
| Contingência | Modos UF/modelo documentados; runbook ensaiado |
| Observabilidade | Auditoria fiscal + correlation ID nas transmissões |

## 2. Gate de negócio / fiscal

| Item | GO se… |
|------|--------|
| Contador | Validou CFOP/CST e cenários críticos |
| Homologação | H01–H20 sem bloqueadores abertos |
| Migração | Sem emissão retroativa informal |
| Treinamento | Operadores treinados em monitor, CC-e, cancelamento, contingência |

## 3. Gate de segurança

| Item | GO se… |
|------|--------|
| Secrets | Fora do repositório; rotação definida |
| Acesso | `FISCAL_GLOBAL_ACCESS` apenas papéis autorizados |
| IDOR | Testes de loja cruzada passando |
| Logs | Sem senha/chave privada/XML integral |

## 4. Plano de cutover (sugerido)

1. Freeze de emissão no sistema legado (horário acordado).
2. Import final de histórico externo (delta).
3. Ajuste fino de sequência.
4. Ativar emissão SystemCommerce em **uma** loja piloto.
5. Monitorar 24–72h; expandir lojas.
6. Manter legado em leitura-only pelo período de hiper-cuidado.

## 5. Rollback

- Desativar emissão (`FiscalGenerationSettings` / flags por loja).
- Não reutilizar números já autorizados.
- Contingência e consulta de protocolo antes de qualquer retransmissão.

## 6. Decisão

| Campo | Valor |
|-------|-------|
| Decisão | ☐ GO ☐ NO-GO |
| Data | |
| Responsável | |
| Justificativa NO-GO | |
