package br.com.systemcommerce.fiscal.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CancellationRequestCreateDto(
        @NotNull UUID documentId,
        @NotBlank @Size(min = 15, max = 500) String justification,
        @NotBlank @Size(max = 100) String idempotencyKey) {}
