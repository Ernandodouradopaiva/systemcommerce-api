package br.com.systemcommerce.inventory.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Saldo consolidado de um produto (soma no backend por lojas/depósitos). */
public record InventoryConsolidatedBalanceResponse(
        UUID productId,
        String productSku,
        String productName,
        BigDecimal physicalQuantity,
        BigDecimal reservedQuantity,
        BigDecimal blockedQuantity,
        BigDecimal inTransitQuantity,
        BigDecimal availableQuantity,
        List<InventoryBalanceResponse> balancesByWarehouse) {}
