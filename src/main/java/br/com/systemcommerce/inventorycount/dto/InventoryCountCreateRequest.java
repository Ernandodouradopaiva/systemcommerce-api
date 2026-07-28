package br.com.systemcommerce.inventorycount.dto;

import br.com.systemcommerce.inventorycount.entity.InventoryCountStatus;
import br.com.systemcommerce.inventorycount.entity.InventoryCountType;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InventoryCountCreateRequest(
        @NotNull UUID storeId,
        @NotNull UUID warehouseId,
        @NotNull InventoryCountType countType,
        Boolean freezeBalances,
        Boolean hideTheoreticalQty,
        Boolean requireSecondCount,
        UUID categoryId,
        UUID brandId,
        UUID storageLocationId,
        Instant plannedAt,
        String notes,
        String idempotencyKey,
        List<InventoryCountItemCreateRequest> items) {}
