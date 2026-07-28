package br.com.systemcommerce.fiscal.distribution.dto;

import java.time.Instant;
import java.util.UUID;

public record DfeDistributionQueryResponse(
        UUID id,
        UUID establishmentId,
        Long requestedNsu,
        Long ultNsu,
        Long maxNsu,
        String cstat,
        String xmotivo,
        String status,
        Integer documentsCount,
        Long latencyMs,
        Instant startedAt,
        Instant finishedAt) {}
