package br.com.systemcommerce.quote.dto;

import br.com.systemcommerce.quote.entity.Quote;
import java.time.Instant;
import java.util.UUID;

public record QuoteStatusHistoryResponse(
        UUID id,
        Quote.QuoteStatus fromStatus,
        Quote.QuoteStatus toStatus,
        String notes,
        Instant changedAt,
        UUID changedBy) {}
