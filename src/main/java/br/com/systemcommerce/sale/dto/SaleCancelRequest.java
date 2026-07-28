package br.com.systemcommerce.sale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaleCancelRequest(
        @NotBlank(message = "Motivo do cancelamento é obrigatório") @Size(max = 500) String reason) {}
