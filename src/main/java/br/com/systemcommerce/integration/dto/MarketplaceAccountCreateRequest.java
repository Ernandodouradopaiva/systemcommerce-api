package br.com.systemcommerce.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record MarketplaceAccountCreateRequest(
        @NotNull UUID organizationId,
        @NotNull UUID salesChannelId,
        @NotNull UUID storeId,
        @NotNull UUID warehouseId,
        @Size(max = 120) String externalAccountId,
        @NotBlank @Size(max = 160) String displayName,
        /** JSON de credenciais em plaintext — cifrado na API, nunca devolvido. */
        String credentialsJson,
        String settingsJson,
        @Size(max = 40) String adapterCode) {}
