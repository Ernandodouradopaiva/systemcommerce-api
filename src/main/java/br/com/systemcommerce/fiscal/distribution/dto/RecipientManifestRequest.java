package br.com.systemcommerce.fiscal.distribution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RecipientManifestRequest(
        @NotNull UUID establishmentId,
        @NotBlank String accessKey,
        @NotBlank String eventType,
        String justification,
        @NotBlank String idempotencyKey,
        UUID distributionDocumentId) {}
