package br.com.systemcommerce.fiscal.establishment.dto;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import jakarta.validation.constraints.NotNull;

public record FiscalEstablishmentEnvironmentRequest(
        @NotNull FiscalEstablishment.FiscalEnvironment environment) {}
