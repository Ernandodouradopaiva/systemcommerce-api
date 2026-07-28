package br.com.systemcommerce.finance.approval.dto;

import br.com.systemcommerce.finance.approval.entity.FinancialApprovalRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ApprovalDtos {
    private ApprovalDtos() {}

    public record PolicyUpdateRequest(
            Boolean requirePaymentApproval,
            BigDecimal paymentApprovalThreshold,
            Boolean requireReversalApproval,
            Boolean requireDiscountApproval,
            BigDecimal discountApprovalThreshold,
            Boolean requireTransferApproval,
            BigDecimal transferApprovalThreshold,
            Boolean requirePeriodReopenApproval,
            Boolean requireManualEntryApproval,
            BigDecimal manualEntryApprovalThreshold) {}

    public record PolicyResponse(
            UUID id,
            UUID organizationId,
            Boolean requirePaymentApproval,
            BigDecimal paymentApprovalThreshold,
            Boolean requireReversalApproval,
            Boolean requireDiscountApproval,
            BigDecimal discountApprovalThreshold,
            Boolean requireTransferApproval,
            BigDecimal transferApprovalThreshold,
            Boolean requirePeriodReopenApproval,
            Boolean requireManualEntryApproval,
            BigDecimal manualEntryApprovalThreshold) {}

    public record CreateApprovalRequest(
            @NotNull UUID organizationId,
            UUID storeId,
            @NotNull FinancialApprovalRequest.OperationType operationType,
            @NotBlank String sourceEntityType,
            UUID sourceEntityId,
            BigDecimal amount,
            String reason,
            String payloadJson,
            @NotBlank String idempotencyKey) {}

    public record DecideRequest(@NotBlank String decision, String notes) {}

    public record ApprovalResponse(
            UUID id,
            UUID organizationId,
            String operationType,
            String status,
            String sourceEntityType,
            UUID sourceEntityId,
            BigDecimal amount,
            String reason,
            Instant requestedAt,
            UUID requestedBy,
            Instant decidedAt,
            UUID decidedBy,
            String decisionNotes) {}
}
