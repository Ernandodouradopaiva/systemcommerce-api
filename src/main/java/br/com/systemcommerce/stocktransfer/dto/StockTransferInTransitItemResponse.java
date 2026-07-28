package br.com.systemcommerce.stocktransfer.dto;

import br.com.systemcommerce.stocktransfer.entity.StockTransferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockTransferInTransitItemResponse(
        UUID transferId,
        String transferNumber,
        StockTransferStatus transferStatus,
        UUID itemId,
        UUID productId,
        String productSku,
        String productName,
        UUID originStoreId,
        String originStoreCode,
        UUID destinationStoreId,
        String destinationStoreCode,
        BigDecimal quantityDispatched,
        BigDecimal quantityReceived,
        BigDecimal quantityPendingReceive,
        Instant dispatchedAt) {}
