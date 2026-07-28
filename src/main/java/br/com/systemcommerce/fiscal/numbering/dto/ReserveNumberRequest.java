package br.com.systemcommerce.fiscal.numbering.dto;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ReserveNumberRequest(
        @NotNull UUID establishmentId,
        @NotBlank @Size(max = 10) String model,
        @NotBlank @Size(max = 10) String series,
        @NotNull FiscalEstablishment.FiscalEnvironment environment,
        UUID documentId,
        @Size(max = 100) String idempotencyKey) {}
