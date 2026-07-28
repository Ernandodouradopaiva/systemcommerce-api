package br.com.systemcommerce.purchase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SupplierReturnItemResponse(
        UUID id,
        UUID productId,
        String productName,
        UUID purchaseOrderItemId,
        UUID purchaseReceiptItemId,
        Integer lineNumber,
        BigDecimal quantity,
        BigDecimal unitCost,
        String batchCode,
        LocalDate expiryDate,
        String serialNumber,
        String notes) {}
