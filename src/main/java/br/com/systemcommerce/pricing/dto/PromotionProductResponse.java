package br.com.systemcommerce.pricing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PromotionProductResponse(
        UUID id,
        UUID promotionId,
        String promotionCode,
        UUID productId,
        String productSku,
        String productName,
        BigDecimal promotionalPrice,
        BigDecimal minQuantity,
        Instant createdAt,
        Instant updatedAt,
        Long version) {}
