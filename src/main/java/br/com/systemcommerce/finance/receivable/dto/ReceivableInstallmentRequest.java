package br.com.systemcommerce.finance.receivable.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceivableInstallmentRequest(
        @NotNull Integer installmentNumber,
        @NotNull LocalDate dueDate,
        @NotNull @DecimalMin("0.01") BigDecimal originalAmount,
        String nossoNumero,
        String billingCode,
        String pixTxid,
        String boletoNumber,
        String notes) {}
