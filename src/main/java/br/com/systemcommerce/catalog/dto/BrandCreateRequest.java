package br.com.systemcommerce.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record BrandCreateRequest(
        UUID organizationId,
        @NotBlank(message = "código é obrigatório") @Size(max = 40) String code,
        @NotBlank(message = "nome é obrigatório") @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @Size(max = 2) String countryCode,
        @Size(max = 255) String website,
        @Size(max = 500) String logoUrl) {}
