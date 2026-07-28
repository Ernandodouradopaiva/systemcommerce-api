package br.com.systemcommerce.fiscal.numbering.dto;

import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberGap.GapStatus;
import java.time.Instant;
import java.util.UUID;

public record FiscalNumberGapResponse(
        UUID id,
        UUID sequenceId,
        Long fromNumber,
        Long toNumber,
        Instant detectedAt,
        String reason,
        GapStatus status,
        String notes) {}
