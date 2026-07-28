package br.com.systemcommerce.fiscal.event.cce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CorrectionLetterCreateRequest(
        @NotNull UUID documentId,
        @NotBlank @Size(min = 15, max = 1000) String correctionText,
        @NotBlank @Size(max = 100) String idempotencyKey) {}
