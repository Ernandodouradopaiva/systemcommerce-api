package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.PromotionBenefit;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PromotionBenefitRequest(
        @NotNull(message = "tipo de benefício é obrigatório") PromotionBenefit.BenefitType benefitType,
        BigDecimal percentValue,
        BigDecimal fixedValue,
        BigDecimal promoUnitPrice,
        BigDecimal buyQuantity,
        BigDecimal payQuantity,
        BigDecimal maxBenefitAmount) {}
