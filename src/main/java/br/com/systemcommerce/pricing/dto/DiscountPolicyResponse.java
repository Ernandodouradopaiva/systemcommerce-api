package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.DiscountPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DiscountPolicyResponse(
        UUID id,
        String code,
        String name,
        String description,
        DiscountPolicy.AppliesTo appliesTo,
        UUID productId,
        String productSku,
        UUID categoryId,
        String categoryName,
        BigDecimal maxPercent,
        BigDecimal maxAmount,
        Integer priority,
        DiscountPolicy.Status status,
        Instant validFrom,
        Instant validTo,
        Instant createdAt,
        Instant updatedAt,
        Long version) {}
