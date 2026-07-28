package br.com.systemcommerce.pricing.dto;

import java.math.BigDecimal;
import java.util.List;

public record PromotionEngineResultResponse(
        BigDecimal subtotal,
        BigDecimal totalDiscount,
        BigDecimal total,
        List<PromotionApplicationResult> applications,
        boolean couponApplied,
        String couponRejectionReason) {}
