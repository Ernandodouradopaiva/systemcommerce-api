package br.com.systemcommerce.fiscal.document.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FiscalDocumentPaymentRequest(
        @Size(max = 10) String paymentMethodFiscalCode,
        @NotNull BigDecimal amount,
        @Size(max = 10) String indicator) {}
