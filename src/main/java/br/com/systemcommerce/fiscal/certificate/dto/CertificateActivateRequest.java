package br.com.systemcommerce.fiscal.certificate.dto;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CertificateActivateRequest(
        @NotNull UUID establishmentId, @NotNull FiscalEstablishment.FiscalEnvironment environment) {}
