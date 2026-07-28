package br.com.systemcommerce.finance.incomestatement.entity;

import br.com.systemcommerce.finance.account.entity.FinancialAccount;
import br.com.systemcommerce.finance.account.entity.FinancialCategory;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "income_statement_mappings")
public class IncomeStatementMapping extends AuditableEntity {

    public enum SourceType {
        CATEGORY,
        ACCOUNT,
        SALES_GROSS,
        SALES_DISCOUNT,
        SALES_CANCELLED,
        COGS_MAPPED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "layout_id", nullable = false)
    private IncomeStatementLayout layout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_id", nullable = false)
    private IncomeStatementLine line;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_category_id")
    private FinancialCategory financialCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_account_id")
    private FinancialAccount financialAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private SourceType sourceType = SourceType.CATEGORY;
}
