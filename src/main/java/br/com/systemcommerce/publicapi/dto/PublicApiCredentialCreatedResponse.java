package br.com.systemcommerce.publicapi.dto;

import java.time.Instant;
import java.util.UUID;

/** Secret em plaintext só na criação — nunca em listagens. */
public record PublicApiCredentialCreatedResponse(
        UUID id,
        UUID organizationId,
        String clientId,
        String clientSecret,
        String name,
        String scopes,
        Integer rateLimitPerMinute) {}
