package br.com.systemcommerce.purchase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseReceiptItemResponse(
        UUID id,
        UUID purchaseOrderItemId,
        UUID productId,
        String productName,
        BigDecimal quantityOrdered,
        BigDecimal quantityPreviouslyReceived,
        BigDecimal quantityReceived,
        BigDecimal quantityRejected,
        BigDecimal quantityAccepted,
        BigDecimal quantityDivergent,
        BigDecimal unitCost,
        String batchCode,
        LocalDate expiryDate,
        String serialNumber,
        String destinationLocation) {}
