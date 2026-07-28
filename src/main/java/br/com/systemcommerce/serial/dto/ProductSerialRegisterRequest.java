package br.com.systemcommerce.serial.dto;

import br.com.systemcommerce.serial.entity.ProductSerialStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProductSerialRegisterRequest(
        @NotNull UUID organizationId,
        @NotNull UUID productId,
        @NotBlank String serialNumber,
        UUID productBatchId,
        UUID warehouseId,
        UUID storageLocationId,
        UUID purchaseReceiptId,
        String notes) {}
