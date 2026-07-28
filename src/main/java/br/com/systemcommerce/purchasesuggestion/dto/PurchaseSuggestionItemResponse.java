package br.com.systemcommerce.purchasesuggestion.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseSuggestionItemResponse(
        UUID id,
        UUID productId,
        String productSku,
        String productName,
        UUID supplierId,
        BigDecimal onHandQty,
        BigDecimal availableQty,
        BigDecimal inTransitQty,
        BigDecimal openPoQty,
        BigDecimal avgDailyConsumption,
        BigDecimal coverageDays,
        BigDecimal reorderPoint,
        BigDecimal maxStock,
        BigDecimal suggestedQty,
        BigDecimal confidenceLevel,
        String justification) {}
