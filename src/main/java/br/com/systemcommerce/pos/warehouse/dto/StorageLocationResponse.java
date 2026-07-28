package br.com.systemcommerce.pos.warehouse.dto;

import br.com.systemcommerce.pos.warehouse.entity.StorageLocation;
import java.util.UUID;

public record StorageLocationResponse(
        UUID id,
        UUID warehouseId,
        UUID zoneId,
        UUID aisleId,
        UUID rackId,
        UUID shelfId,
        String code,
        String barcode,
        StorageLocation.LocationStatus status,
        Boolean trackBalance,
        Boolean active) {}
