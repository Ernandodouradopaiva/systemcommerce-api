package br.com.systemcommerce.purchase.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SupplierQuotationResponseResponse(
        UUID id,
        UUID supplierId,
        String supplierName,
        String paymentCondition,
        BigDecimal freightAmount,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        Integer leadTimeDays,
        LocalDate validUntil,
        String notes,
        BigDecimal totalAmount,
        Instant submittedAt,
        boolean locked,
        List<SupplierQuotationResponseItemResponse> items) {}
