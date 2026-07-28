package br.com.systemcommerce.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerBlockRequest(
        @NotBlank(message = "motivo do bloqueio é obrigatório") @Size(max = 500) String reason) {}
