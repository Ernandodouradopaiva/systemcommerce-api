package br.com.systemcommerce.integration.dto;

import java.util.UUID;

public record ChannelProductResponse(
        UUID id,
        UUID marketplaceAccountId,
        UUID productId,
        String externalProductId,
        String externalSku,
        String syncStatus) {}
