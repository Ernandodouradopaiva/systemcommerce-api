package br.com.systemcommerce.finance.account.mapper;

import br.com.systemcommerce.finance.account.dto.FinancialAccountResponse;
import br.com.systemcommerce.finance.account.dto.FinancialCategoryResponse;
import br.com.systemcommerce.finance.account.entity.FinancialAccount;
import br.com.systemcommerce.finance.account.entity.FinancialCategory;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FinancialAccountMapper {

    public FinancialAccountResponse toResponse(FinancialAccount account) {
        return toResponse(account, List.of());
    }

    public FinancialAccountResponse toResponse(FinancialAccount account, List<FinancialAccountResponse> children) {
        return new FinancialAccountResponse(
                account.getId(),
                account.getOrganization() != null ? account.getOrganization().getId() : null,
                account.getCode(),
                account.getName(),
                account.getDescription(),
                account.getParent() != null ? account.getParent().getId() : null,
                account.getParent() != null ? account.getParent().getCode() : null,
                account.getLevelNo(),
                account.getAccountType(),
                account.getNature(),
                Boolean.TRUE.equals(account.getAcceptsPosting()),
                Boolean.TRUE.equals(account.getRequiresCostCenter()),
                account.getStatus(),
                account.isUsable(),
                account.getSortOrder(),
                account.getVersion(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                children);
    }

    public FinancialCategoryResponse toCategoryResponse(FinancialCategory category) {
        return new FinancialCategoryResponse(
                category.getId(),
                category.getOrganization() != null ? category.getOrganization().getId() : null,
                category.getCode(),
                category.getName(),
                category.getDescription(),
                category.getFinancialAccount() != null ? category.getFinancialAccount().getId() : null,
                category.getFinancialAccount() != null ? category.getFinancialAccount().getCode() : null,
                category.getUsageScope(),
                category.getStatus(),
                category.isUsable(),
                category.getVersion(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
