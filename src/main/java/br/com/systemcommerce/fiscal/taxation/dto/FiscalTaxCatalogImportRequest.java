package br.com.systemcommerce.fiscal.taxation.dto;

import br.com.systemcommerce.fiscal.taxation.entity.FiscalTaxCatalog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FiscalTaxCatalogImportRequest(
        @NotNull FiscalTaxCatalog.CatalogType catalogType,
        @NotBlank @Size(max = 40) String versionCode,
        @Size(max = 100) String source,
        @Size(max = 2000) String notes,
        @NotEmpty @Valid List<FiscalTaxCatalogImportEntry> entries) {}
