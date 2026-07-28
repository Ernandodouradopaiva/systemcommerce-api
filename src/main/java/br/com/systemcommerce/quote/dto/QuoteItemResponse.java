package br.com.systemcommerce.quote.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record QuoteItemResponse(
        UUID id,
        UUID productId,
        String productName,
        Integer lineNumber,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountAmount,
        BigDecimal lineSubtotal,
        BigDecimal lineTotal,
        BigDecimal quantityConverted,
        BigDecimal remainingToConvert,
        String priceOrigin) {}
