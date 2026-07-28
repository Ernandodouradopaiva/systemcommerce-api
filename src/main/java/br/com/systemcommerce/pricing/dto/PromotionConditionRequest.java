package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.PromotionCondition;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PromotionConditionRequest(
        @NotNull(message = "tipo de condição é obrigatório") PromotionCondition.ConditionType conditionType,
        UUID referenceId,
        BigDecimal minQuantity,
        BigDecimal minAmount,
        String configJson) {}
