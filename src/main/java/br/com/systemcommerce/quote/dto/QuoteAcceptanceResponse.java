package br.com.systemcommerce.quote.dto;

import java.time.Instant;
import java.util.UUID;

public record QuoteAcceptanceResponse(
        UUID id,
        UUID quoteId,
        Instant acceptedAt,
        String acceptedByName,
        String acceptedByEmail,
        String acceptanceToken,
        String channel,
        String notes) {}
