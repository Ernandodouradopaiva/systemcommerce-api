package br.com.systemcommerce.inventorycount.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryCountItemResponse(
        UUID id,
        Integer lineNumber,
        UUID productId,
        String productSku,
        String productName,
        UUID storageLocationId,
        String storageLocationCode,
        BigDecimal theoreticalQuantity,
        BigDecimal countedQuantity1,
        BigDecimal countedQuantity2,
        BigDecimal finalCountedQuantity,
        BigDecimal varianceQuantity,
        BigDecimal unitCost,
        Boolean frozen,
        String notes) {}
