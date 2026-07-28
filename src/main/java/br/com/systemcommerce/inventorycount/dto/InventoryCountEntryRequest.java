package br.com.systemcommerce.inventorycount.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record InventoryCountEntryRequest(
        @NotNull UUID itemId,
        @NotNull @DecimalMin("0") BigDecimal quantity,
        @NotNull @Min(1) @Max(2) Integer countPass,
        String barcode,
        String idempotencyKey) {}
