package br.com.systemcommerce.purchase.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseRequestItemResponse(
        UUID id,
        UUID productId,
        String productName,
        Integer lineNumber,
        String description,
        BigDecimal quantityRequested,
        BigDecimal quantityApproved,
        BigDecimal quantityConverted,
        BigDecimal pendingQuantity,
        String unit,
        BigDecimal currentStockInfo,
        BigDecimal minimumStock,
        String justification,
        UUID suggestedSupplierId,
        String suggestedSupplierName) {}
