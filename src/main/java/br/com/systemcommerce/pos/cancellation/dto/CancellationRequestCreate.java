package br.com.systemcommerce.pos.cancellation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CancellationRequestCreate(
        @NotNull(message = "venda é obrigatória") UUID saleId,
        @NotBlank(message = "motivo é obrigatório") @Size(max = 500) String reason) {}
