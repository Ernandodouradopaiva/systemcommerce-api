# Carga DEMO (somente testes)

Script: `demo_load.sql`

## O que carrega

- Usuários: `gerente`, `caixa`, `vendedor`, `estoque` (senha **Demo@123**)
- Role `CASHIER`, acessos às lojas LOJA-01/LOJA-02
- Terminal `TERM-02`, produtos extras, estoque completo, preços
- Consumidor final, vínculos cliente×loja, funcionários/vendedores
- Promoções, comissões, metas, entrada e transferência de estoque
- Sessão de caixa **fechada** + venda histórica `DEMO-0001` (PAID)

## Como aplicar (banco remoto já configurado no `.env`)

```bash
docker run --rm \
  -e PGPASSWORD="$POSTGRES_PASSWORD" \
  -v "$PWD/src/main/resources/db/demo/demo_load.sql:/demo_load.sql:ro" \
  postgres:16-alpine \
  psql -h "$POSTGRES_HOST" -p 5432 -U postgres -d systemcommerce -v ON_ERROR_STOP=1 -f /demo_load.sql
```

No Windows (PowerShell), a partir de `SystemCommerce-api`:

```powershell
docker run --rm -e PGPASSWORD=$env:POSTGRES_PASSWORD `
  -v "${PWD}/src/main/resources/db/demo/demo_load.sql:/demo_load.sql:ro" `
  postgres:16-alpine `
  psql -h $env:POSTGRES_HOST -p 5432 -U postgres -d systemcommerce -v ON_ERROR_STOP=1 -f /demo_load.sql
```

**Não** é migration Flyway — não sobe sozinho e não deve ir para produção.
