package br.com.systemcommerce.inventorycount.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record InventoryCountItemCreateRequest(
        @NotNull UUID productId, UUID storageLocationId, BigDecimal theoreticalQuantity, String notes) {}
