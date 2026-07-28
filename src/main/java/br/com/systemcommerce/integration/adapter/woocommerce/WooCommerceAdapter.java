package br.com.systemcommerce.integration.adapter.woocommerce;

import br.com.systemcommerce.integration.adapter.MarketplaceAdapter;
import br.com.systemcommerce.integration.entity.MarketplaceAccount;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Adapter WooCommerce (Prompt 85) — canal virtual; pedidos → SalesOrder via hub. */
@Component
@RequiredArgsConstructor
public class WooCommerceAdapter implements MarketplaceAdapter {

    private final ObjectMapper objectMapper;

    @Override
    public String adapterCode() {
        return "WOOCOMMERCE";
    }

    @Override
    public boolean supports(String channelTypeOrAdapterCode) {
        return adapterCode().equalsIgnoreCase(channelTypeOrAdapterCode) || "WOO".equalsIgnoreCase(channelTypeOrAdapterCode);
    }

    @Override
    public AdapterAuthResult authenticate(MarketplaceAccount account, String credentialsJson) {
        try {
            JsonNode node = objectMapper.readTree(credentialsJson == null ? "{}" : credentialsJson);
            String key = node.path("consumer_key").asText(null);
            String secret = node.path("consumer_secret").asText(null);
            if (key == null || secret == null) {
                throw new IllegalArgumentException("consumer_key/consumer_secret obrigatórios");
            }
            return new AdapterAuthResult(key, secret, null);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Credenciais WooCommerce inválidas", ex);
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
            List<ExternalOrderItem> items = new ArrayList<>();
            for (JsonNode li : root.path("line_items")) {
                items.add(new ExternalOrderItem(
                        li.path("id").asText(null),
                        li.path("product_id").asText(null),
                        li.path("sku").asText(null),
                        li.path("name").asText(null),
                        BigDecimal.valueOf(li.path("quantity").asDouble(1)),
                        new BigDecimal(li.path("price").asText("0"))));
            }
            JsonNode billing = root.path("billing");
            String buyerName = (billing.path("first_name").asText("") + " " + billing.path("last_name").asText("")).trim();
            return new ExternalOrder(
                    root.path("id").asText(),
                    root.path("status").asText(null),
                    root.path("customer_id").asText(null),
                    buyerName.isBlank() ? null : buyerName,
                    new BigDecimal(root.path("total").asText("0")),
                    root.path("currency").asText("BRL"),
                    json,
                    items);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Payload WooCommerce inválido", ex);
        }
    }
}
