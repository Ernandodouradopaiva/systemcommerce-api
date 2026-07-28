package br.com.systemcommerce.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record IntegrationJobCreateRequest(
        @NotNull UUID organizationId,
        UUID marketplaceAccountId,
        @NotBlank String jobType,
        String payloadJson,
        Integer maxAttempts) {}
