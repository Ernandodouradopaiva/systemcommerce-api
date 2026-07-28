package br.com.systemcommerce.pricing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PromotionApplicationResult(
        UUID promotionId, String promotionCode, String promotionName, BigDecimal benefitAmount, String description) {}
