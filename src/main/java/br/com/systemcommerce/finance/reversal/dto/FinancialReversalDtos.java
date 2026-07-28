package br.com.systemcommerce.finance.reversal.dto;

import br.com.systemcommerce.finance.reversal.entity.FinancialReversal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FinancialReversalDtos {
    private FinancialReversalDtos() {}

    public record CreateRequest(
            @NotNull UUID organizationId,
            UUID storeId,
            @NotNull FinancialReversal.SourceType sourceType,
            @NotNull UUID sourceDocumentId,
            @NotBlank String reason,
            String notes,
            String idempotencyKey) {}

    public record ItemResponse(
            UUID id,
            String itemType,
            UUID originalMovementId,
            UUID reversalMovementId,
            BigDecimal originalAmount,
            BigDecimal reversedAmount,
            UUID targetInstallmentId) {}

    public record Response(
            UUID id,
            UUID organizationId,
            UUID storeId,
            FinancialReversal.SourceType sourceType,
            UUID sourceDocumentId,
            String reason,
            FinancialReversal.Status status,
            Boolean partial,
            String notes,
            List<ItemResponse> items,
            Long version,
            Instant createdAt) {}
}
