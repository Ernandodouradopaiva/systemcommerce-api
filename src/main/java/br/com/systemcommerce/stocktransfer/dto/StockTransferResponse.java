package br.com.systemcommerce.stocktransfer.dto;

import br.com.systemcommerce.stocktransfer.entity.StockTransferStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StockTransferResponse(
        UUID id,
        UUID organizationId,
        String number,
        UUID originStoreId,
        String originStoreCode,
        UUID originWarehouseId,
        String originWarehouseCode,
        UUID destinationStoreId,
        String destinationStoreCode,
        UUID destinationWarehouseId,
        String destinationWarehouseCode,
        UUID requesterId,
        UUID approverId,
        UUID dispatcherId,
        UUID receiverId,
        Instant requestedAt,
        Instant dispatchedAt,
        Instant receivedAt,
        StockTransferStatus status,
        String observation,
        String reason,
        List<StockTransferItemResponse> items,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
