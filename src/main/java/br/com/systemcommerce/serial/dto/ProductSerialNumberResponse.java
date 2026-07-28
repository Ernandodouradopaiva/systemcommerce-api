package br.com.systemcommerce.serial.dto;

import br.com.systemcommerce.serial.entity.ProductSerialStatus;
import java.util.UUID;

public record ProductSerialNumberResponse(
        UUID id,
        UUID organizationId,
        UUID productId,
        String productSku,
        String serialNumber,
        UUID productBatchId,
        UUID warehouseId,
        ProductSerialStatus status,
        String notes,
        Boolean active) {}
