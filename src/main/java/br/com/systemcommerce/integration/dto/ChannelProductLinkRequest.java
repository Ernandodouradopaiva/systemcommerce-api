package br.com.systemcommerce.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChannelProductLinkRequest(
        @NotNull UUID marketplaceAccountId,
        @NotNull UUID productId,
        @NotBlank String externalProductId,
        String externalSku) {}
