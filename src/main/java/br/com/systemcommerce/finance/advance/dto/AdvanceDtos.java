package br.com.systemcommerce.finance.advance.dto;

import br.com.systemcommerce.finance.advance.entity.AdvanceApplication;
import br.com.systemcommerce.finance.advance.entity.CustomerAdvance;
import br.com.systemcommerce.finance.advance.entity.SupplierAdvance;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class AdvanceDtos {
    private AdvanceDtos() {}

    public record CustomerAdvanceCreateRequest(
            @NotNull UUID organizationId,
            UUID storeId,
            @NotNull UUID customerId,
            @NotNull UUID holderId,
            String documentNumber,
            @NotNull LocalDate advanceDate,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            String notes,
            @NotBlank String idempotencyKey) {}

    public record SupplierAdvanceCreateRequest(
            @NotNull UUID organizationId,
            UUID storeId,
            @NotNull UUID supplierId,
            @NotNull UUID holderId,
            String documentNumber,
            @NotNull LocalDate advanceDate,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            String notes,
            @NotBlank String idempotencyKey) {}

    public record AdvanceApplyRequest(
            @NotNull UUID organizationId,
            UUID customerAdvanceId,
            UUID supplierAdvanceId,
            @NotNull AdvanceApplication.TargetType targetType,
            @NotNull UUID targetDocumentId,
            UUID targetInstallmentId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull LocalDate applicationDate,
            String notes,
            @NotBlank String idempotencyKey) {}

    public record AdvanceRefundRequest(
            @NotNull UUID organizationId,
            UUID customerAdvanceId,
            UUID supplierAdvanceId,
            @NotNull UUID holderId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull LocalDate refundDate,
            @NotBlank String reason,
            @NotBlank String idempotencyKey) {}

    public record AdvanceCancelRequest(@NotBlank String reason) {}

    public record CustomerAdvanceResponse(
            UUID id,
            UUID organizationId,
            UUID storeId,
            UUID customerId,
            String customerName,
            UUID holderId,
            String documentNumber,
            LocalDate advanceDate,
            BigDecimal originalAmount,
            BigDecimal appliedAmount,
            BigDecimal refundedAmount,
            BigDecimal balanceAmount,
            CustomerAdvance.Status status,
            String notes,
            Long version,
            Instant createdAt) {}

    public record SupplierAdvanceResponse(
            UUID id,
            UUID organizationId,
            UUID storeId,
            UUID supplierId,
            String supplierName,
            UUID holderId,
            String documentNumber,
            LocalDate advanceDate,
            BigDecimal originalAmount,
            BigDecimal appliedAmount,
            BigDecimal refundedAmount,
            BigDecimal balanceAmount,
            SupplierAdvance.Status status,
            String notes,
            Long version,
            Instant createdAt) {}

    public record AdvanceBalanceResponse(UUID id, BigDecimal originalAmount, BigDecimal appliedAmount, BigDecimal refundedAmount, BigDecimal balanceAmount, String status) {}
}
