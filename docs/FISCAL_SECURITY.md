# Segurança do módulo fiscal — SystemCommerce

> Complementa [FISCAL_ARCHITECTURE.md](./FISCAL_ARCHITECTURE.md).  
> Alinha-se a padrões de `finance.security` e auditoria compartilhada.

---

## 1. Princípios

1. **Certificado nunca no navegador** — upload/admin apenas via API autenticada; uso só no backend ou **agente fiscal** em rede confiável.
2. **Senhas/PINs de certificado nunca em texto puro** — criptografia envelope (KMS/secret store / chave mestra da aplicação) + rotação.
3. **CSC e tokens SEFAZ** — mesmo tratamento de segredo.
4. **Assinatura XML** — exclusivamente server-side (ou agente); frontend recebe no máximo status/chave/DANFE.
5. **Least privilege** — permissões `FISCAL_*` granulares; cancelamento/inutilização podem exigir aprovação em 2 etapas.
6. **Auditoria completa** — quem emitiu, cancelou, rotacionou certificado, alterou ambiente homolog→prod.
7. **Sanitização** — logs sem dump de XML completo com dados sensíveis em nível INFO; XML completo só em storage controlado.
8. **TLS** mútuo/cliente conforme exigência do webservice da UF.
9. **Ambientes isolados** — homologação e produção com certificados, CSC, numeração e endpoints separados.
10. **Multiloja** — certificado e CSC amarrados ao estabelecimento da loja; sem vazamento cross-org.

---

## 2. Certificados

| Aspecto | Decisão |
|---|---|
| Formatos | A1 (arquivo) na 1ª entrega; A3 via agente seguro (interface `SigningPort`) |
| Armazenamento | Blob cifrado + metadados (`alias`, validade, CNPJ, thumbprint) |
| Senha | Secret separado, cifrado; nunca em `application.yml` commitado |
| Uso | Carregado em memória apenas no momento da assinatura |
| Expiração | Monitor alerta (dashboard fiscal) |
| Troca | Novo registro; documentos antigos preservam thumbprint usado |

`SigningPort`:

- `sign(xmlBytes, establishmentId)` → `SignedXml`
- Implementações: `LocalA1SigningAdapter`, `RemoteFiscalAgentAdapter`, `NoOpSigningAdapter` (testes)

---

## 3. Frontend

Proibido:

- calcular impostos oficiais;
- manter `.pfx` / senha em `localStorage` / IndexedDB;
- montar XML;
- chamar URLs SEFAZ diretamente.

Permitido:

- formulários de cadastro que **enviam** arquivo de certificado via multipart à API (usuário com `FISCAL_CERTIFICATE_MANAGE`);
- exibir validade/CNPJ mascarado;
- baixar DANFE/XML autorizado se permissão de leitura (XML pode ser restrito a perfis fiscais).

---

## 4. Aprovação em duas etapas

Reutilizar padrão `finance.approval`:

- Políticas por organização/loja: cancelamento de NF autorizada, inutilização de faixa, ativação de contingência, troca homolog→prod.
- `FiscalApprovalRequest` com vínculo ao `FiscalDocument` / operação.

---

## 5. Auditoria e compliance

Eventos sugeridos (`event_code`):

- `FISCAL_DOCUMENT_EMITTED`, `FISCAL_DOCUMENT_AUTHORIZED`, `FISCAL_DOCUMENT_REJECTED`
- `FISCAL_DOCUMENT_CANCELLED`, `FISCAL_CCE_SENT`, `FISCAL_INUTILIZED`
- `FISCAL_CERTIFICATE_UPLOADED`, `FISCAL_CERTIFICATE_ROTATED`
- `FISCAL_ENVIRONMENT_CHANGED`, `FISCAL_CONTINGENCY_STARTED|ENDED`
- `FISCAL_XML_DOWNLOADED`

Campos: org, loja, usuário, correlation id, document id, chave (quando houver), outcome.

---

## 6. Critérios de aceite

- [x] Certificado/senha fora do browser e fora de plain text
- [x] Assinatura no backend/agente
- [x] Ambientes separados
- [x] Auditoria e permissões definidas
- [x] Aprovação opcional para operações sensíveis
