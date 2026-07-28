package br.com.systemcommerce.seller.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SellerDiscountLimitResponse(
        UUID sellerProfileId,
        UUID storeId,
        BigDecimal profileMaxDiscountPercent,
        BigDecimal storeMaxDiscountPercent,
        BigDecimal effectiveMaxDiscountPercent) {}
