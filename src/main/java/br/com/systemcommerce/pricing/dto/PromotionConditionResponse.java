package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.PromotionCondition;
import java.math.BigDecimal;
import java.util.UUID;

public record PromotionConditionResponse(
        UUID id,
        UUID promotionId,
        PromotionCondition.ConditionType conditionType,
        UUID referenceId,
        BigDecimal minQuantity,
        BigDecimal minAmount,
        String configJson) {}
