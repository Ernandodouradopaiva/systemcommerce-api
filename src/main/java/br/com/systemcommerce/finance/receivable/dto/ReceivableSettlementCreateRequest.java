package br.com.systemcommerce.finance.receivable.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReceivableSettlementCreateRequest(
        @NotNull UUID organizationId,
        UUID storeId,
        @NotNull UUID holderId,
        UUID cashSessionId,
        UUID paymentMethodId,
        @NotNull LocalDate paymentDate,
        LocalDate effectiveDate,
        BigDecimal feeAmount,
        BigDecimal grossAmount,
        BigDecimal acquirerFeeAmount,
        String referenceCode,
        String externalReference,
        String notes,
        @NotBlank String idempotencyKey,
        Boolean confirmImmediately,
        @NotEmpty @Valid List<ReceivableSettlementAllocationRequest> allocations) {}
