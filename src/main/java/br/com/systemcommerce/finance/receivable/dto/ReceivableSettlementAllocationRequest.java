package br.com.systemcommerce.finance.receivable.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ReceivableSettlementAllocationRequest(
        @NotNull UUID installmentId,
        @NotNull @DecimalMin("0.01") BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal fineAmount,
        BigDecimal discountAmount) {}
