package br.com.systemcommerce.integration.dto;

import br.com.systemcommerce.integration.entity.IntegrationJobStatus;
import java.time.Instant;
import java.util.UUID;

public record IntegrationJobResponse(
        UUID id,
        UUID organizationId,
        UUID marketplaceAccountId,
        String jobType,
        IntegrationJobStatus status,
        Integer attemptCount,
        Integer maxAttempts,
        Instant nextAttemptAt,
        String lastError,
        Instant startedAt,
        Instant finishedAt) {}
