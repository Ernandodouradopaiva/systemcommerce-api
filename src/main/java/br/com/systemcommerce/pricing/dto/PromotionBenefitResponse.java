package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.PromotionBenefit;
import java.math.BigDecimal;
import java.util.UUID;

public record PromotionBenefitResponse(
        UUID id,
        UUID promotionId,
        PromotionBenefit.BenefitType benefitType,
        BigDecimal percentValue,
        BigDecimal fixedValue,
        BigDecimal promoUnitPrice,
        BigDecimal buyQuantity,
        BigDecimal payQuantity,
        BigDecimal maxBenefitAmount) {}
