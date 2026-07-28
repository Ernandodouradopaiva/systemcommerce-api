package br.com.systemcommerce.integration.adapter.mercadolivre;

import br.com.systemcommerce.integration.adapter.MarketplaceAdapter;
import br.com.systemcommerce.integration.entity.MarketplaceAccount;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adapter Mercado Livre (Prompt 83) — regras específicas isoladas neste pacote.
 */
@Component
@RequiredArgsConstructor
public class MercadoLivreAdapter implements MarketplaceAdapter {

    private final ObjectMapper objectMapper;

    @Override
    public String adapterCode() {
        return "MERCADO_LIVRE";
    }

    @Override
    public boolean supports(String channelTypeOrAdapterCode) {
        return adapterCode().equalsIgnoreCase(channelTypeOrAdapterCode) || "ML".equalsIgnoreCase(channelTypeOrAdapterCode);
    }

    @Override
    public AdapterAuthResult authenticate(MarketplaceAccount account, String credentialsJson) {
        // Em produção: OAuth ML. Aqui valida JSON mínimo e devolve token mockável.
        try {
            JsonNode node = objectMapper.readTree(credentialsJson == null ? "{}" : credentialsJson);
            String access = node.path("access_token").asText("ml-access-token");
            String refresh = node.path("refresh_token").asText(null);
            return new AdapterAuthResult(access, refresh, 21600L);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Credenciais Mercado Livre inválidas", ex);
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
    public void publishStock(MarketplaceAccount account, String externalListingId, BigDecimal availableQty) {
        // HTTP real via RestClient em evolução; contrato isolado do domínio.
    }

    @Override
    public void publishPrice(MarketplaceAccount account, String externalListingId, BigDecimal price) {
        // no-op até wiring HTTP
    }

    /** Parse de payload ML → ExternalOrder (testável por contrato). */
    public ExternalOrder mapOrderPayload(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<ExternalOrderItem> items = new ArrayList<>();
            for (JsonNode oi : root.path("order_items")) {
                JsonNode item = oi.path("item");
                items.add(new ExternalOrderItem(
                        oi.path("item").path("id").asText(null),
                        item.path("id").asText(null),
                        item.path("seller_sku").asText(item.path("id").asText(null)),
                        item.path("title").asText(null),
                        BigDecimal.valueOf(oi.path("quantity").asDouble(1)),
                        BigDecimal.valueOf(oi.path("unit_price").asDouble(0))));
            }
            return new ExternalOrder(
                    root.path("id").asText(),
                    root.path("status").asText(null),
                    root.path("buyer").path("id").asText(null),
                    root.path("buyer").path("nickname").asText(null),
                    BigDecimal.valueOf(root.path("total_amount").asDouble(0)),
                    root.path("currency_id").asText("BRL"),
                    json,
                    items);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Payload Mercado Livre inválido", ex);
        }
    }
}
