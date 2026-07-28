package br.com.systemcommerce.fiscal.validation.dto;

import br.com.systemcommerce.fiscal.validation.entity.FiscalSchemaVersion.SchemaStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record FiscalSchemaCreateRequest(
        @NotBlank @Size(max = 10) String model,
        @NotBlank @Size(max = 40) String layoutVersion,
        @Size(max = 200) String schemaNamespace,
        @Size(max = 500) String xsdResourcePath,
        String xsdContent,
        LocalDate validFrom,
        LocalDate validUntil,
        SchemaStatus status) {}
