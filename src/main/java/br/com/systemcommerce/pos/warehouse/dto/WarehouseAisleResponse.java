package br.com.systemcommerce.pos.warehouse.dto;

import java.util.UUID;

public record WarehouseAisleResponse(UUID id, UUID zoneId, String code, String name, Boolean active) {}
