package br.com.systemcommerce.publicapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PublicApiCredentialCreateRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 1000) String scopes,
        Integer rateLimitPerMinute) {}
