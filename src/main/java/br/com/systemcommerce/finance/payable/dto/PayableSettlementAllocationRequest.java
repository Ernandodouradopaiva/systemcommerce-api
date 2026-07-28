package br.com.systemcommerce.finance.payable.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PayableSettlementAllocationRequest(
        @NotNull UUID installmentId,
        @NotNull @DecimalMin("0.01") BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal fineAmount,
        BigDecimal discountAmount) {}
