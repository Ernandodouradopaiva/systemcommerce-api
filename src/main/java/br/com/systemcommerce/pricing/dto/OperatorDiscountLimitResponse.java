package br.com.systemcommerce.pricing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OperatorDiscountLimitResponse(
        UUID id,
        UUID roleId,
        String roleCode,
        String roleName,
        BigDecimal maxPercent,
        BigDecimal maxAmount,
        Instant createdAt,
        Instant updatedAt,
        Long version) {}
