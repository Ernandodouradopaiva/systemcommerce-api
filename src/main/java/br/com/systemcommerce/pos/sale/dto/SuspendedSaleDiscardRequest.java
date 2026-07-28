package br.com.systemcommerce.pos.sale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SuspendedSaleDiscardRequest(
        @NotNull UUID cashSessionId,
        @NotBlank @Size(max = 500) String reason,
        Long expectedVersion) {}
