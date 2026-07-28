package br.com.systemcommerce.finance.paymentcatalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record InstallmentRequest(
        @NotNull Integer sequenceNo,
        @NotNull Integer daysOffset,
        @NotNull @DecimalMin("0.0001") BigDecimal percentage) {}
