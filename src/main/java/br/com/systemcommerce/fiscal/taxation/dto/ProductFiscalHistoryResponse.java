package br.com.systemcommerce.fiscal.taxation.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductFiscalHistoryResponse(
        UUID id,
        UUID productId,
        UUID profileId,
        Instant changedAt,
        UUID changedBy,
        String changeType,
        String snapshotJson) {}
