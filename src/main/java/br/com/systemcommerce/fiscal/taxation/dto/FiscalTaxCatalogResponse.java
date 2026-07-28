package br.com.systemcommerce.fiscal.taxation.dto;

import br.com.systemcommerce.fiscal.taxation.entity.FiscalTaxCatalog;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FiscalTaxCatalogResponse(
        UUID id,
        FiscalTaxCatalog.CatalogType catalogType,
        String code,
        String description,
        String uf,
        String extraJson,
        LocalDate validFrom,
        LocalDate validUntil,
        String catalogVersion,
        String source,
        FiscalTaxCatalog.CatalogStatus status,
        boolean validNow,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
