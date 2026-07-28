package br.com.systemcommerce.purchase.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SupplierQuotationResponseItemResponse(
        UUID id,
        UUID quotationItemId,
        BigDecimal unitPrice,
        BigDecimal quantityAvailable,
        BigDecimal freightAmount,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        Integer leadTimeDays,
        String brandOffered,
        BigDecimal lineTotal,
        boolean selected,
        BigDecimal quantitySelected,
        String notes) {}
