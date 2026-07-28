package br.com.systemcommerce.inventorycount.dto;

import br.com.systemcommerce.inventorycount.entity.InventoryCountStatus;
import br.com.systemcommerce.inventorycount.entity.InventoryCountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InventoryCountResponse(
        UUID id,
        String countNumber,
        InventoryCountType countType,
        InventoryCountStatus status,
        UUID storeId,
        String storeName,
        UUID warehouseId,
        String warehouseCode,
        Boolean freezeBalances,
        Boolean hideTheoreticalQty,
        Boolean requireSecondCount,
        Instant plannedAt,
        Instant openedAt,
        Instant closedAt,
        Instant postedAt,
        String notes,
        List<InventoryCountItemResponse> items) {}
