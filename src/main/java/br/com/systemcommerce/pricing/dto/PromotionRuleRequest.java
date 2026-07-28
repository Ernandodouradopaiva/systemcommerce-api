package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.PromotionRule;
import jakarta.validation.constraints.NotNull;

public record PromotionRuleRequest(
        @NotNull(message = "tipo de regra é obrigatório") PromotionRule.RuleType ruleType,
        String configJson,
        Integer sortOrder) {}
