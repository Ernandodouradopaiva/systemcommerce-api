package br.com.systemcommerce.quote.dto;

import java.time.Instant;
import java.util.UUID;

public record QuoteRevisionResponse(
        UUID id,
        UUID quoteId,
        Integer revisionNumber,
        String snapshotJson,
        String changeNotes,
        Instant createdAt,
        UUID createdByUserId) {}
