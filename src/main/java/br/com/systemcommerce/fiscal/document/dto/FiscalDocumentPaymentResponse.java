package br.com.systemcommerce.fiscal.document.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FiscalDocumentPaymentResponse(
        UUID id, String paymentMethodFiscalCode, BigDecimal amount, String indicator) {}
