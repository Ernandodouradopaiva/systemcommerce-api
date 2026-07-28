package br.com.systemcommerce.pos.warehouse.dto;

import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import java.time.Instant;
import java.util.UUID;

public record WarehouseResponse(
        UUID id,
        UUID storeId,
        String storeCode,
        String storeName,
        String code,
        String name,
        Boolean allowsSale,
        Warehouse.WarehouseStatus status,
        Warehouse.WarehouseType warehouseType,
        Boolean central,
        Boolean virtualWarehouse,
        Boolean blockedForMovement,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
