package br.com.systemcommerce.purchase.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseQuotationItemResponse(
        UUID id,
        UUID purchaseRequestItemId,
        UUID productId,
        String productName,
        Integer lineNumber,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal quantitySelected,
        BigDecimal pendingSelection) {}
