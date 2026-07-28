package br.com.systemcommerce.inventory.dto;

import br.com.systemcommerce.inventory.entity.InventoryMovement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryMovementResponse(
        UUID id,
        UUID productId,
        String productSku,
        String productName,
        UUID storeId,
        String storeCode,
        UUID warehouseId,
        String warehouseCode,
        InventoryMovement.MovementType type,
        BigDecimal quantity,
        BigDecimal previousBalance,
        BigDecimal newBalance,
        String origin,
        UUID originId,
        String reason,
        UUID adjustmentReasonId,
        String adjustmentReasonCode,
        String adjustmentReasonDescription,
        String observation,
        UUID userId,
        String userName,
        Instant createdAt) {}
