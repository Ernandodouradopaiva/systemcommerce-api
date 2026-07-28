package br.com.systemcommerce.pricing.dto;

import br.com.systemcommerce.pricing.entity.Coupon;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CouponUpdateRequest(
        UUID promotionId,
        @Size(max = 500) String description,
        Integer maxUses,
        Integer maxUsesPerCustomer,
        Instant validFrom,
        Instant validUntil,
        @NotNull(message = "status é obrigatório") Coupon.Status status) {}
