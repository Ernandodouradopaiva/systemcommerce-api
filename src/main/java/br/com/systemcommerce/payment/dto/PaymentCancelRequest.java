package br.com.systemcommerce.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentCancelRequest(
        @NotBlank(message = "Motivo do cancelamento é obrigatório") @Size(max = 500) String reason) {}
