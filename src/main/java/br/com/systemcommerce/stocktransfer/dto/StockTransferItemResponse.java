package br.com.systemcommerce.stocktransfer.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StockTransferItemResponse(
        UUID id,
        UUID productId,
        String productSku,
        String productName,
        String unitOfMeasure,
        BigDecimal quantityRequested,
        BigDecimal quantityApproved,
        BigDecimal quantityDispatched,
        BigDecimal quantityReceived,
        BigDecimal quantityDivergent,
        BigDecimal quantityPendingReceive,
        String observation,
        Long version) {}
