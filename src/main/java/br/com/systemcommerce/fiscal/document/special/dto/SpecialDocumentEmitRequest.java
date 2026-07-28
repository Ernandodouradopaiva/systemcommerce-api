package br.com.systemcommerce.fiscal.document.special.dto;

import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SpecialDocumentEmitRequest(
        @NotNull UUID refDocumentId,
        @NotEmpty @Valid List<FiscalDocumentItemRequest> items,
        @NotBlank @Size(max = 500) String justification,
        @NotBlank @Size(max = 100) String idempotencyKey) {}
