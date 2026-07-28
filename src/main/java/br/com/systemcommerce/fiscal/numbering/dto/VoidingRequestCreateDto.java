package br.com.systemcommerce.fiscal.numbering.dto;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record VoidingRequestCreateDto(
        @NotNull UUID establishmentId,
        @NotBlank @Size(max = 10) String model,
        @NotBlank @Size(max = 10) String series,
        @NotNull FiscalEstablishment.FiscalEnvironment environment,
        @NotNull Long fromNumber,
        @NotNull Long toNumber,
        @NotBlank @Size(min = 15, max = 500) String justification,
        @NotBlank @Size(max = 100) String idempotencyKey) {}
