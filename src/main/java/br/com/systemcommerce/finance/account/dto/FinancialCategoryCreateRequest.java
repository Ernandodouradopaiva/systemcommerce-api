package br.com.systemcommerce.finance.account.dto;

import br.com.systemcommerce.finance.account.entity.FinancialCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record FinancialCategoryCreateRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        UUID financialAccountId,
        @NotNull FinancialCategory.UsageScope usageScope) {}
