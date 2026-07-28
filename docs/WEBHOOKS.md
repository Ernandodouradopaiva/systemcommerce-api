# Webhooks e eventos (Prompt 82)

## Modelo

1. Domínio grava `IntegrationOutboxEvent` na **mesma transação** (`OutboxPublisher`)  
2. Worker agenda `WebhookDelivery` por subscription  
3. `WebhookDispatcher` entrega HTTP fora da TX principal (HMAC-SHA256)  

## Eventos iniciais

`product.created`, `product.updated`, `inventory.changed`, `price.changed`, `customer.created`,
`sales-order.created`, `sales-order.updated`, `sale.completed`, `payment.confirmed`,
`purchase-order.created`, `goods-receipt.posted`, `shipment.updated`

Hoje `sales-order.created` é publicado em `SalesOrderService.create`.

## Entidades

`WebhookSubscription`, `WebhookSecret`, `WebhookDelivery`, `WebhookAttempt`, `IntegrationOutboxEvent`

## Regras

- Assinatura header `X-SystemCommerce-Signature` (HMAC-SHA256 hex)  
- Retry com backoff exponencial  
- Timeout HTTP 10s  
- Desativação após `maxFailures` consecutivas  
- Replay: `POST /api/v1/webhooks/deliveries/{id}/replay`  
- Payload versionado (`payload_version`)  
- Entrega idempotente (`subscription_id` + `idempotency_key`)  

Permissões: `WEBHOOK_READ` / `WEBHOOK_MANAGE`.
