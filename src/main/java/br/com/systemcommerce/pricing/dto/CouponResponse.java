package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.Coupon;
import java.time.Instant;
import java.util.UUID;

public record CouponResponse(
        UUID id,
        UUID organizationId,
        UUID promotionId,
        String promotionCode,
        String code,
        String description,
        Integer maxUses,
        Integer maxUsesPerCustomer,
        Integer usedCount,
        Instant validFrom,
        Instant validUntil,
        Coupon.Status status,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Long version) {}
