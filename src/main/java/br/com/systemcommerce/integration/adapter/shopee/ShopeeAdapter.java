package br.com.systemcommerce.integration.adapter.shopee;

import br.com.systemcommerce.integration.adapter.MarketplaceAdapter;
import br.com.systemcommerce.integration.entity.MarketplaceAccount;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Adapter Shopee (Prompt 84) — isolado do domínio central. */
@Component
@RequiredArgsConstructor
public class ShopeeAdapter implements MarketplaceAdapter {

    private final ObjectMapper objectMapper;

    @Override
    public String adapterCode() {
        return "SHOPEE";
    }

    @Override
    public boolean supports(String channelTypeOrAdapterCode) {
        return adapterCode().equalsIgnoreCase(channelTypeOrAdapterCode);
    }

    @Override
    public AdapterAuthResult authenticate(MarketplaceAccount account, String credentialsJson) {
        try {
            JsonNode node = objectMapper.readTree(credentialsJson == null ? "{}" : credentialsJson);
            return new AdapterAuthResult(
                    node.path("access_token").asText("shopee-access-token"),
                    node.path("refresh_token").asText(null),
                    14400L);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Credenciais Shopee inválidas", ex);
        }
    }

    @Override
    public List<ExternalListing> fetchListings(MarketplaceAccount account, String cursor) {
        return List.of();
    }

    @Override
    public List<ExternalOrder> fetchOrders(MarketplaceAccount account, String cursor) {
        return List.of();
    }

    @Override
    public void publishStock(MarketplaceAccount account, String externalListingId, BigDecimal availableQty) {}

    @Override
    public void publishPrice(MarketplaceAccount account, String externalListingId, BigDecimal price) {}

    public ExternalOrder mapOrderPayload(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode order = root.has("order") ? root.path("order") : root;
            List<ExternalOrderItem> items = new ArrayList<>();
            for (JsonNode li : order.path("item_list")) {
                items.add(new ExternalOrderItem(
                        li.path("order_item_id").asText(null),
                        li.path("item_id").asText(null),
                        li.path("item_sku").asText(null),
                        li.path("item_name").asText(null),
                        BigDecimal.valueOf(li.path("model_quantity_purchased").asDouble(1)),
                        BigDecimal.valueOf(li.path("model_discounted_price").asDouble(0))));
            }
            return new ExternalOrder(
                    order.path("order_sn").asText(),
                    order.path("order_status").asText(null),
                    order.path("buyer_user_id").asText(null),
                    order.path("buyer_username").asText(null),
                    BigDecimal.valueOf(order.path("total_amount").asDouble(0)),
                    "BRL",
                    json,
                    items);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Payload Shopee inválido", ex);
        }
    }
}
