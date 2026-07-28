package br.com.systemcommerce.inventory.dto;

import java.util.UUID;

public record InventoryAdjustmentReasonResponse(UUID id, String code, String description, Boolean active) {}
