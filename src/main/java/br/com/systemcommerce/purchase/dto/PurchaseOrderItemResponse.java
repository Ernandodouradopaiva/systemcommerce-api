package br.com.systemcommerce.purchase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseOrderItemResponse(
        UUID id,
        UUID productId,
        String productName,
        Integer lineNumber,
        String description,
        BigDecimal quantityOrdered,
        BigDecimal quantityReceived,
        BigDecimal quantityCancelled,
        BigDecimal unitCost,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal lineTotal,
        LocalDate expectedDate) {}
