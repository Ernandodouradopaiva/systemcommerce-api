package br.com.systemcommerce.finance.payable.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PayableInstallmentRequest(
        @NotNull Integer installmentNumber,
        @NotNull LocalDate dueDate,
        @NotNull @DecimalMin("0.01") BigDecimal originalAmount,
        String barcode,
        String digitableLine,
        String referenceCode) {}
