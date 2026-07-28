package br.com.systemcommerce.pos.warehouse.dto;

import br.com.systemcommerce.pos.warehouse.entity.WarehouseZone;
import java.util.UUID;

public record WarehouseZoneResponse(
        UUID id, UUID warehouseId, String code, String name, WarehouseZone.ZoneStatus status, Boolean active) {}
