# Arquitetura mobile (Prompt 86)

## Princípios

1. **API é a única fonte da verdade** — o app não calcula totais, estoque, elegibilidade nem preços oficiais.  
2. Mesmo backend `SystemCommerce-api`; autenticação JWT + **refresh rotativo** já existente (`/api/v1/auth/login|refresh`).  
3. Secrets (tokens, refresh) só em **secure storage** (Keychain / EncryptedSharedPreferences) — nunca em AsyncStorage plain.  
4. Nenhuma regra comercial no aplicativo.

## Autenticação mobile

| Item | Estratégia |
|---|---|
| Access token | JWT curto (~15 min) |
| Refresh | Rotação obrigatória; logout-all no servidor |
| Biometria | Desbloqueia o secure store local; não substitui o JWT |
| Multi-loja | Header/contexto de loja já usado no ERP (`StoreContext`) |

## Notificações

- Registro: `POST /api/v1/mobile/device-tokens` (`DevicePushToken`, V219)  
- Plataformas: `ANDROID` / `IOS` / `WEB`  
- Entrega FCM/APNs: fase seguinte (worker); tokens já persistidos  

## Funcionalidades prioritárias (fase 1)

Dashboard; consulta produtos/estoque; pedidos; aprovação compras/descontos; separação; recebimento;
inventário; vendedores/metas; notificações.

## Offline limitado

- Cache de leitura (produtos, saldos exibidos) com TTL curto  
- Fila local de **intents** (nunca confirmação comercial offline)  
- Sync ao reconectar via endpoints idempotentes (`Idempotency-Key`)  

## Câmera / códigos

- Captura de barcode/QR no device → envia código para API (`products` / PDV / inventário)  
- Decisão de produto/preço/estoque **sempre** na API  

## Contratos de API (mobile usa os mesmos do ERP)

- Auth: `/api/v1/auth/*`  
- Produtos, estoque, pedidos, compras, inventário, separação — endpoints `/api/v1/**` existentes  
- Push: `/api/v1/mobile/device-tokens`  

## Responsividade web

O front React deve permanecer usável em viewport estreito (listagens/detalhes). App nativo é cliente HTTP
separado — não duplicar projetos por plataforma no monorepo atual.
