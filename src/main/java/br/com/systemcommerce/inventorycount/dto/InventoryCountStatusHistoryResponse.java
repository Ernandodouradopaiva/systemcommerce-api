package br.com.systemcommerce.inventorycount.dto;

import br.com.systemcommerce.inventorycount.entity.InventoryCountStatus;
import java.time.Instant;

public record InventoryCountStatusHistoryResponse(
        InventoryCountStatus fromStatus, InventoryCountStatus toStatus, String notes, Instant changedAt) {}
