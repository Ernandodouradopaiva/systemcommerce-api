package br.com.systemcommerce.fiscal.taxation.dto;

import br.com.systemcommerce.fiscal.taxation.entity.FiscalTaxCatalog;
import java.time.Instant;
import java.util.UUID;

public record FiscalTaxCatalogVersionResponse(
        UUID id,
        FiscalTaxCatalog.CatalogType catalogType,
        String versionCode,
        String source,
        Instant importedAt,
        UUID importedBy,
        String notes,
        Integer entryCount) {}
