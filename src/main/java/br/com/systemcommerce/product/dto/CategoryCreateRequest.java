package br.com.systemcommerce.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CategoryCreateRequest(
        @NotBlank(message = "nome é obrigatório") @Size(max = 120) String name,
        @Size(max = 500) String description,
        UUID parentId) {}
