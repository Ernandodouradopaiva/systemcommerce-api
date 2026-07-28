package br.com.systemcommerce.salesorder.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SalesOrderItemResponse(
        UUID id,
        UUID productId,
        String productName,
        Integer lineNumber,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountAmount,
        BigDecimal lineSubtotal,
        BigDecimal lineTotal) {}
