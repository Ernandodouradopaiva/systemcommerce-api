package br.com.systemcommerce.quote.dto;

import br.com.systemcommerce.quote.entity.Quote;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuoteResponse(
        UUID id,
        String quoteNumber,
        UUID organizationId,
        UUID storeId,
        String storeCode,
        UUID customerId,
        String customerName,
        UUID sellerId,
        String sellerName,
        UUID sellerProfileId,
        String sellerProfileCode,
        UUID priceTableId,
        String priceTableCode,
        String channel,
        String paymentCondition,
        String carrierName,
        LocalDate expectedDeliveryDate,
        Integer validityDays,
        Quote.QuoteStatus status,
        LocalDate validUntil,
        String notes,
        boolean reserveStock,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal freightAmount,
        BigDecimal surchargeAmount,
        BigDecimal totalAmount,
        Integer revisionNumber,
        UUID convertedSalesOrderId,
        List<QuoteItemResponse> items,
        boolean canEdit,
        boolean canCancel,
        boolean canConvert,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
