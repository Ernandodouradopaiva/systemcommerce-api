package br.com.systemcommerce.storeproduct.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreProductBlockRequest(
        @NotBlank(message = "motivo do bloqueio é obrigatório") @Size(max = 500) String reason) {}
