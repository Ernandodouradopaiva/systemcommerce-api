package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.PromotionRule;
import java.util.UUID;

public record PromotionRuleResponse(
        UUID id, UUID promotionId, PromotionRule.RuleType ruleType, String configJson, Integer sortOrder) {}
