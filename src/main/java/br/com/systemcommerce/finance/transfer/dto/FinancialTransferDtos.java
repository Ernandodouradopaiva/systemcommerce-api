package br.com.systemcommerce.finance.transfer.dto;

import br.com.systemcommerce.finance.transfer.entity.FinancialTransfer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class FinancialTransferDtos {
    private FinancialTransferDtos() {}

    public record CreateRequest(
            @NotNull UUID organizationId,
            @NotNull UUID sourceHolderId,
            @NotNull UUID targetHolderId,
            UUID sourceStoreId,
            UUID targetStoreId,
            UUID cashSessionId,
            @NotNull LocalDate transferDate,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            BigDecimal feeAmount,
            @NotBlank String reason,
            String referenceCode,
            String notes,
            String idempotencyKey) {}

    public record Response(
            UUID id,
            UUID organizationId,
            UUID sourceHolderId,
            UUID targetHolderId,
            UUID sourceStoreId,
            UUID targetStoreId,
            UUID cashSessionId,
            LocalDate transferDate,
            BigDecimal amount,
            BigDecimal feeAmount,
            String reason,
            String referenceCode,
            FinancialTransfer.Status status,
            UUID sourceMovementId,
            UUID targetMovementId,
            UUID feeMovementId,
            UUID reverseOfId,
            String notes,
            Long version,
            Instant createdAt) {}
}
