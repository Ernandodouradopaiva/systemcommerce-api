package br.com.systemcommerce.pos.warehouse.dto;

import java.util.UUID;

public record WarehouseRackResponse(UUID id, UUID aisleId, String code, String name, Boolean active) {}
