package br.com.systemcommerce.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentRefundRequest(
        @NotBlank(message = "Motivo do estorno é obrigatório") @Size(max = 500) String reason) {}
