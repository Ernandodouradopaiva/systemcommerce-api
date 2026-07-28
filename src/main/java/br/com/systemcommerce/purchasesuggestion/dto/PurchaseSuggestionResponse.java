package br.com.systemcommerce.purchasesuggestion.dto;

import br.com.systemcommerce.purchasesuggestion.entity.PurchaseSuggestionExecutionType;
import br.com.systemcommerce.purchasesuggestion.entity.PurchaseSuggestionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PurchaseSuggestionResponse(
        UUID id,
        UUID executionId,
        UUID storeId,
        UUID warehouseId,
        UUID supplierId,
        PurchaseSuggestionStatus status,
        Integer totalItems,
        BigDecimal totalSuggestedQty,
        Instant createdAt,
        List<PurchaseSuggestionItemResponse> items) {}
