package br.com.systemcommerce.stockentry.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StockEntryItemResponse(
        UUID id,
        UUID productId,
        String productSku,
        String productName,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal lineTotal) {}
