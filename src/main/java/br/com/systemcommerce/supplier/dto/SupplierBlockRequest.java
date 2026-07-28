package br.com.systemcommerce.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierBlockRequest(@NotBlank(message = "motivo do bloqueio é obrigatório") @Size(max = 500) String reason) {}
