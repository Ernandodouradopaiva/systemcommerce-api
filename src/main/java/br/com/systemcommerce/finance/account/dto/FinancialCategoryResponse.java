package br.com.systemcommerce.finance.account.dto;

import br.com.systemcommerce.finance.account.entity.FinancialCategory;
import java.time.Instant;
import java.util.UUID;

public record FinancialCategoryResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        String description,
        UUID financialAccountId,
        String financialAccountCode,
        FinancialCategory.UsageScope usageScope,
        FinancialCategory.CategoryStatus status,
        boolean usable,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
