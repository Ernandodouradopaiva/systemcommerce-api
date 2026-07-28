package br.com.systemcommerce.integration.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.com.systemcommerce.integration.adapter.mercadolivre.MercadoLivreAdapter;
import br.com.systemcommerce.integration.adapter.shopee.ShopeeAdapter;
import br.com.systemcommerce.integration.adapter.woocommerce.WooCommerceAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MarketplaceAdapterContractTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mercadoLivreMapsOrder() {
        var adapter = new MercadoLivreAdapter(mapper);
        var order = adapter.mapOrderPayload("""
                {"id":"ML123","status":"paid","total_amount":100.5,"currency_id":"BRL",
                 "buyer":{"id":"B1","nickname":"buyer"},
                 "order_items":[{"quantity":2,"unit_price":50.25,"item":{"id":"P1","title":"Item","seller_sku":"SKU1"}}]}
                """);
        assertEquals("ML123", order.externalOrderId());
        assertEquals(1, order.items().size());
        assertEquals("SKU1", order.items().getFirst().externalSku());
    }

    @Test
    void shopeeMapsOrder() {
        var adapter = new ShopeeAdapter(mapper);
        var order = adapter.mapOrderPayload("""
                {"order_sn":"SP999","order_status":"READY_TO_SHIP","total_amount":80,
                 "buyer_user_id":"U1","buyer_username":"shopper",
                 "item_list":[{"order_item_id":"1","item_id":"I1","item_sku":"S1","item_name":"Prod",
                   "model_quantity_purchased":1,"model_discounted_price":80}]}
                """);
        assertEquals("SP999", order.externalOrderId());
        assertFalse(order.items().isEmpty());
    }

    @Test
    void wooCommerceMapsOrder() {
        var adapter = new WooCommerceAdapter(mapper);
        var order = adapter.mapOrderPayload("""
                {"id":"55","status":"processing","total":"120.00","currency":"BRL","customer_id":"9",
                 "billing":{"first_name":"Ana","last_name":"Silva"},
                 "line_items":[{"id":1,"product_id":10,"sku":"W1","name":"Kit","quantity":1,"price":"120.00"}]}
                """);
        assertEquals("55", order.externalOrderId());
        assertEquals("Ana Silva", order.buyerName());
        assertEquals(1, order.items().size());
    }
}
