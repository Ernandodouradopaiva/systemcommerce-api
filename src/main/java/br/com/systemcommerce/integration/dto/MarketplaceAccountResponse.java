package br.com.systemcommerce.integration.dto;

import br.com.systemcommerce.integration.entity.MarketplaceAccountStatus;
import java.time.Instant;
import java.util.UUID;

public record MarketplaceAccountResponse(
        UUID id,
        UUID organizationId,
        UUID salesChannelId,
        UUID storeId,
        UUID warehouseId,
        String externalAccountId,
        String displayName,
        MarketplaceAccountStatus status,
        String adapterCode,
        Instant lastSyncAt,
        Boolean credentialsConfigured) {}
