package br.com.systemcommerce.fiscal.taxation.dto;

import br.com.systemcommerce.fiscal.taxation.entity.FiscalTaxCatalog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record FiscalTaxCatalogImportEntry(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 500) String description,
        @Size(max = 2) String uf,
        String extraJson,
        @NotNull LocalDate validFrom,
        LocalDate validUntil) {}
