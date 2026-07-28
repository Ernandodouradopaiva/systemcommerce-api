package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.StoreGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StoreGroupUpdateRequest(
        @NotBlank(message = "nome é obrigatório") @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotNull(message = "status é obrigatório") StoreGroup.Status status) {}
