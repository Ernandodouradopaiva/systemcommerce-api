package br.com.systemcommerce.finance.receivable.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReceivableCreateRequest(
        @NotNull UUID organizationId,
        UUID storeId,
        @NotNull UUID customerId,
        UUID paymentConditionId,
        UUID financialCategoryId,
        UUID costCenterId,
        String documentNumber,
        @NotNull LocalDate issueDate,
        @NotNull LocalDate competenceDate,
        @NotNull @DecimalMin("0.01") BigDecimal originalAmount,
        BigDecimal plannedDiscount,
        BigDecimal plannedAddition,
        String notes,
        String idempotencyKey,
        Boolean openImmediately,
        @NotEmpty @Valid List<ReceivableInstallmentRequest> installments) {}
