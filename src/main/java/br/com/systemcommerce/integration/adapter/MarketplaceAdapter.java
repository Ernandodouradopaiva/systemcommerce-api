package br.com.systemcommerce.integration.adapter;

import br.com.systemcommerce.integration.entity.MarketplaceAccount;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Contrato genérico de marketplace (Prompt 80). Adapters específicos (ML/Shopee/Woo) implementam
 * este contrato — regras de canal NÃO entram no domínio central.
 */
public interface MarketplaceAdapter {

    String adapterCode();

    boolean supports(String channelTypeOrAdapterCode);

    AdapterAuthResult authenticate(MarketplaceAccount account, String credentialsJson);

    List<ExternalListing> fetchListings(MarketplaceAccount account, String cursor);

    List<ExternalOrder> fetchOrders(MarketplaceAccount account, String cursor);

    void publishStock(MarketplaceAccount account, String externalListingId, BigDecimal availableQty);

    void publishPrice(MarketplaceAccount account, String externalListingId, BigDecimal price);

    record AdapterAuthResult(String accessToken, String refreshToken, Long expiresInSeconds) {}

    record ExternalListing(
            String externalListingId,
            String externalProductId,
            String externalSku,
            String title,
            BigDecimal price,
            BigDecimal quantity,
            String status) {}

    record ExternalOrder(
            String externalOrderId,
            String externalStatus,
            String buyerExternalId,
            String buyerName,
            BigDecimal totalAmount,
            String currency,
            String rawPayloadJson,
            List<ExternalOrderItem> items) {}

    record ExternalOrderItem(
            String externalItemId,
            String externalProductId,
            String externalSku,
            String title,
            BigDecimal quantity,
            BigDecimal unitPrice) {}
}
