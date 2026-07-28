package br.com.systemcommerce.publicapi.dto;

import java.time.Instant;
import java.util.UUID;

public record PublicApiCredentialResponse(
        UUID id,
        UUID organizationId,
        String clientId,
        String name,
        String scopes,
        Integer rateLimitPerMinute,
        Instant revokedAt,
        Instant lastUsedAt,
        Boolean active) {}
