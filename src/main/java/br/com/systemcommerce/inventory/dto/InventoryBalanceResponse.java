package br.com.systemcommerce.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryBalanceResponse(
        UUID inventoryId,
        UUID productId,
        String productSku,
        String productName,
        String unitOfMeasure,
        UUID storeId,
        String storeCode,
        UUID warehouseId,
        String warehouseCode,
        String warehouseName,
        BigDecimal physicalQuantity,
        BigDecimal reservedQuantity,
        BigDecimal blockedQuantity,
        BigDecimal inTransitQuantity,
        BigDecimal availableQuantity,
        BigDecimal minStock,
        BigDecimal maxStock,
        BigDecimal reorderPoint,
        Boolean stockBelowMinimum,
        Boolean allowNegativeStock,
        Long version,
        Instant updatedAt) {}
