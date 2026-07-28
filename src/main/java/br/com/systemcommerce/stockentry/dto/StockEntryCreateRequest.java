package br.com.systemcommerce.stockentry.dto;

import br.com.systemcommerce.stockentry.entity.StockEntryStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record StockEntryCreateRequest(
        UUID organizationId,
        @NotNull UUID storeId,
        @NotNull UUID warehouseId,
        @Size(max = 200) String supplierName,
        @Size(max = 80) String documentNumber,
        @NotNull LocalDate entryDate,
        @Size(max = 2000) String notes) {}
