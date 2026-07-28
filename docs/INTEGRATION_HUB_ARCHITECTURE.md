# Hub de integração com marketplaces (Prompt 80)

## Princípio

O domínio comercial **não** conhece Mercado Livre, Shopee ou WooCommerce. Toda particularidade fica em
`integration.adapter.*` que implementam `MarketplaceAdapter`.

## Entidades

| Entidade | Papel |
|---|---|
| `SalesChannel` | Canal genérico (tipo ML/Shopee/Woo/GENERIC) |
| `MarketplaceAccount` | Conta conectada (loja + depósito + credenciais AES-GCM) |
| `ChannelProduct` | Vínculo produto externo ↔ produto interno |
| `ChannelListing` | Anúncio publicado |
| `ChannelOrder` / `ChannelOrderItem` | Pedido externo idempotente |
| `ChannelEvent` | Evento inbound idempotente |
| `IntegrationJob` | Sync com retry + `FAILED_DEAD_LETTER` |
| `IntegrationError` | Dead-letter / histórico de falhas |
| `SynchronizationCheckpoint` | Cursor de sync |

## Fluxo de pedido

1. Adapter busca/parseia pedido externo  
2. `ChannelOrderIngestionService.ingestExternalOrder` (idempotente por `externalOrderId` / `idempotencyKey`)  
3. `convertToSalesOrder` → `SalesOrderService.createFromIntegration` (reserva conforme conta)  

## Segurança

- Credenciais cifradas com `SecretEncryptionService` (AES-GCM)  
- DTOs **nunca** devolvem plaintext  
- Frontend não recebe secrets  

## Endpoints admin

`/api/v1/sales-channels`, `/marketplace-accounts`, `/channel-products/link`, `/channel-orders`, `/integration-jobs`

Permissões: `INTEGRATION_READ` / `INTEGRATION_MANAGE` (V220).

## Adapters

- Prompt 83 — `MercadoLivreAdapter`  
- Prompt 84 — `ShopeeAdapter`  
- Prompt 85 — `WooCommerceAdapter`  

Cada um expõe `mapOrderPayload` para testes de contrato.
