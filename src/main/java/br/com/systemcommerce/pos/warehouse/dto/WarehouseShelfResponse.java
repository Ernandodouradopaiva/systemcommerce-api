package br.com.systemcommerce.pos.warehouse.dto;

import java.util.UUID;

public record WarehouseShelfResponse(UUID id, UUID rackId, String code, String name, Boolean active) {}
