package br.com.systemcommerce.fiscal.establishment.dto;

import java.time.Instant;
import java.util.UUID;

public record FiscalEstablishmentHistoryResponse(
        UUID id,
        UUID establishmentId,
        Instant changedAt,
        UUID changedBy,
        String changeType,
        String snapshotJson) {}
