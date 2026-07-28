package br.com.systemcommerce.commission.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SalesTargetCreateRequest(
        UUID organizationId,
        UUID sellerProfileId,
        UUID storeId,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        UUID categoryId,
        UUID productId,
        @NotNull BigDecimal targetAmount,
        BigDecimal targetQuantity) {}
