package br.com.systemcommerce.pricing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StoreGroupCreateRequest(
        UUID organizationId,
        @NotBlank(message = "código é obrigatório") @Size(max = 40) String code,
        @NotBlank(message = "nome é obrigatório") @Size(max = 200) String name,
        @Size(max = 1000) String description) {}
