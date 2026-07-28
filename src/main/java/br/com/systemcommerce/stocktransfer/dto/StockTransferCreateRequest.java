package br.com.systemcommerce.stocktransfer.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StockTransferCreateRequest(
        UUID organizationId,
        @NotNull UUID originStoreId,
        @NotNull UUID originWarehouseId,
        @NotNull UUID destinationStoreId,
        @NotNull UUID destinationWarehouseId,
        String observation,
        String reason,
        String idempotencyKey) {}
