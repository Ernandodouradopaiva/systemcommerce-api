package br.com.systemcommerce.stockentry.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record StockEntryItemCreateRequest(
        @NotNull UUID productId,
        @NotNull @DecimalMin(value = "0.001", inclusive = true) BigDecimal quantity,
        @DecimalMin(value = "0", inclusive = true) BigDecimal unitCost,
        @Size(max = 500) String observation) {}
