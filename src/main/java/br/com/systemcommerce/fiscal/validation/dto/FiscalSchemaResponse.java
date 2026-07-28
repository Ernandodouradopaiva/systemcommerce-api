package br.com.systemcommerce.fiscal.validation.dto;

import br.com.systemcommerce.fiscal.validation.entity.FiscalSchemaVersion.SchemaStatus;
import java.time.LocalDate;
import java.util.UUID;

public record FiscalSchemaResponse(
        UUID id,
        String model,
        String layoutVersion,
        String schemaNamespace,
        String xsdResourcePath,
        SchemaStatus status,
        LocalDate validFrom,
        LocalDate validUntil) {}
