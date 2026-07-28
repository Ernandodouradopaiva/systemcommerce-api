package br.com.systemcommerce.finance.renegotiation.dto;

import br.com.systemcommerce.finance.renegotiation.entity.FinancialRenegotiation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class FinancialRenegotiationDtos {
    private FinancialRenegotiationDtos() {}

    public record NewInstallmentRequest(
            @NotNull Integer installmentNumber,
            @NotNull LocalDate dueDate,
            @NotNull @DecimalMin("0.01") BigDecimal amount) {}

    public record CreateRequest(
            @NotNull UUID organizationId,
            UUID storeId,
            @NotNull FinancialRenegotiation.DocumentSide documentSide,
            @NotNull UUID originalDocumentId,
            @NotEmpty List<UUID> installmentIds,
            @NotNull LocalDate renegotiationDate,
            BigDecimal interestAmount,
            BigDecimal penaltyAmount,
            BigDecimal discountAmount,
            BigDecimal downPaymentAmount,
            UUID advanceApplicationId,
            UUID paymentConditionId,
            UUID chargePolicyId,
            @NotEmpty @Valid List<NewInstallmentRequest> newInstallments,
            @NotBlank String reason,
            String notes,
            String idempotencyKey) {}

    public record CancelRequest(@NotBlank String reason) {}

    public record ItemResponse(UUID originalInstallmentId, BigDecimal originalBalance) {}

    public record InstallmentResponse(
            Integer installmentNumber, LocalDate dueDate, BigDecimal amount, UUID generatedInstallmentId) {}

    public record Response(
            UUID id,
            UUID organizationId,
            UUID storeId,
            FinancialRenegotiation.DocumentSide documentSide,
            UUID originalDocumentId,
            UUID newDocumentId,
            FinancialRenegotiation.Status status,
            LocalDate renegotiationDate,
            BigDecimal balanceBefore,
            BigDecimal interestAmount,
            BigDecimal penaltyAmount,
            BigDecimal discountAmount,
            BigDecimal downPaymentAmount,
            BigDecimal newTotalAmount,
            String reason,
            String notes,
            List<ItemResponse> items,
            List<InstallmentResponse> newInstallments,
            Long version,
            Instant createdAt) {}
}
