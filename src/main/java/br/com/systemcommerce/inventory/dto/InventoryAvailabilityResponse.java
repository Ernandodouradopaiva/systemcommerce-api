package br.com.systemcommerce.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Disponibilidade oficial para venda em um depósito/loja. */
public record InventoryAvailabilityResponse(
        UUID productId,
        UUID storeId,
        UUID warehouseId,
        BigDecimal availableQuantity,
        BigDecimal physicalQuantity,
        BigDecimal reservedQuantity,
        BigDecimal blockedQuantity,
        BigDecimal inTransitQuantity,
        boolean availableForSale,
        String message) {}
