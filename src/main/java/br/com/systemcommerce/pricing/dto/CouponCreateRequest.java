package br.com.systemcommerce.pricing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CouponCreateRequest(
        UUID organizationId,
        UUID promotionId,
        @NotBlank(message = "código é obrigatório") @Size(max = 60) String code,
        @Size(max = 500) String description,
        Integer maxUses,
        Integer maxUsesPerCustomer,
        Instant validFrom,
        Instant validUntil) {}
