package br.com.systemcommerce.commission.dto;

import br.com.systemcommerce.commission.entity.SalesTarget.TargetStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SalesTargetResponse(
        UUID id,
        UUID organizationId,
        UUID sellerProfileId,
        String sellerCode,
        UUID storeId,
        String storeCode,
        LocalDate periodStart,
        LocalDate periodEnd,
        UUID categoryId,
        UUID productId,
        BigDecimal targetAmount,
        BigDecimal targetQuantity,
        TargetStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version) {}
