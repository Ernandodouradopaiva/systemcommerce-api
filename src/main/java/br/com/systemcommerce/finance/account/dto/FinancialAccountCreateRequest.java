package br.com.systemcommerce.finance.account.dto;

import br.com.systemcommerce.finance.account.entity.FinancialAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record FinancialAccountCreateRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        UUID parentId,
        @NotNull FinancialAccount.AccountType accountType,
        @NotNull FinancialAccount.Nature nature,
        @NotNull Boolean acceptsPosting,
        Boolean requiresCostCenter,
        Integer sortOrder) {}
