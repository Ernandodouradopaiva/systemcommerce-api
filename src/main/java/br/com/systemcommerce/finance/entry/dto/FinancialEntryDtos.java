package br.com.systemcommerce.finance.entry.dto;

import br.com.systemcommerce.finance.entry.entity.FinancialEntry;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class FinancialEntryDtos {
    private FinancialEntryDtos() {}

    public record CreateRequest(
            @NotNull UUID organizationId,
            UUID storeId,
            @NotNull UUID holderId,
            @NotNull UUID financialCategoryId,
            UUID costCenterId,
            @NotNull FinancialEntry.EntryType entryType,
            @NotNull LocalDate entryDate,
            @NotNull LocalDate competenceDate,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String reason,
            String referenceCode,
            String attachmentUrl,
            String notes,
            String idempotencyKey) {}

    public record UpdateRequest(
            UUID storeId,
            @NotNull UUID holderId,
            @NotNull UUID financialCategoryId,
            UUID costCenterId,
            @NotNull FinancialEntry.EntryType entryType,
            @NotNull LocalDate entryDate,
            @NotNull LocalDate competenceDate,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String reason,
            String referenceCode,
            String attachmentUrl,
            String notes) {}

    public record CancelRequest(@NotBlank String reason) {}

    public record Response(
            UUID id,
            UUID organizationId,
            UUID storeId,
            UUID holderId,
            UUID financialCategoryId,
            UUID costCenterId,
            FinancialEntry.EntryType entryType,
            LocalDate entryDate,
            LocalDate competenceDate,
            BigDecimal amount,
            String reason,
            String referenceCode,
            String attachmentUrl,
            FinancialEntry.Status status,
            UUID holderMovementId,
            UUID reverseOfId,
            String notes,
            Long version,
            Instant createdAt) {}
}
