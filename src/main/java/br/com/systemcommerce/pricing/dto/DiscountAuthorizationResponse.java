package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.DiscountAuthorization;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DiscountAuthorizationResponse(
        UUID id,
        UUID saleId,
        UUID saleItemId,
        BigDecimal requestedAmount,
        BigDecimal requestedPercent,
        DiscountAuthorization.Status status,
        String requestReason,
        String decisionNotes,
        UUID requestedById,
        String requestedByName,
        UUID decidedById,
        String decidedByName,
        Instant decidedAt,
        Instant createdAt,
        Instant updatedAt,
        Long version) {}
