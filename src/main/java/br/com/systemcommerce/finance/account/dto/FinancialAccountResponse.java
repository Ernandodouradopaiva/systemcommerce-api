package br.com.systemcommerce.finance.account.dto;

import br.com.systemcommerce.finance.account.entity.FinancialAccount;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FinancialAccountResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        String description,
        UUID parentId,
        String parentCode,
        Integer levelNo,
        FinancialAccount.AccountType accountType,
        FinancialAccount.Nature nature,
        boolean acceptsPosting,
        boolean requiresCostCenter,
        FinancialAccount.AccountStatus status,
        boolean usable,
        Integer sortOrder,
        Long version,
        Instant createdAt,
        Instant updatedAt,
        List<FinancialAccountResponse> children) {}
