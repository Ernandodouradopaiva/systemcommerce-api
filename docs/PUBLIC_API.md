# API pública SystemCommerce (Prompt 81)

## Base

`/api/public/v1`

Autenticação própria: `POST /api/public/v1/oauth/token` (`client_credentials`) → JWT com
`type=public_access`, claims `api=public`, `org`, `scopes`.

## Credenciais

Gerenciadas em `/api/v1/public-api-credentials` (JWT admin):

- `clientId` / `clientSecret` (secret só na criação; hash BCrypt no banco)  
- escopos CSV (`products.read`, `inventory.read`, `prices.read`, `orders.read`, `stores.read`, `webhooks.read`)  
- rate limit por minuto  
- revogação  

Logs em `public_api_access_logs` **sem** secrets.

## Endpoints iniciais

| Método | Path | Escopo |
|---|---|---|
| POST | `/oauth/token` | — |
| GET | `/products` | `products.read` |
| GET | `/inventory?productId&warehouseId` | `inventory.read` |
| GET | `/prices?productId&storeId` | `prices.read` |
| GET | `/orders` | `orders.read` |
| GET | `/orders/{id}/status` | `orders.read` |
| GET | `/stores` | `stores.read` |
| GET | `/webhooks` | `webhooks.read` |

## Regras

- Isolamento por organização do token  
- Paginação Spring Data  
- Header `Idempotency-Key` registrado em log  
- Não expor campos internos sensíveis  
- Versionamento explícito em `/api/public/v1`
