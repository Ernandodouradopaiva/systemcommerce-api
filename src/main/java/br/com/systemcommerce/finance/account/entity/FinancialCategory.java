package br.com.systemcommerce.finance.account.entity;

import br.com.systemcommerce.organization.entity.Organization;
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

/** Categoria financeira vinculável a conta analítica (Prompt 92). */
@Getter
@Setter
@Entity
@Table(name = "financial_categories")
public class FinancialCategory extends AuditableEntity {

    public enum UsageScope {
        PURCHASE,
        SALE,
        BOTH
    }

    public enum CategoryStatus {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_account_id")
    private FinancialAccount financialAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_scope", nullable = false, length = 20)
    private UsageScope usageScope = UsageScope.BOTH;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CategoryStatus status = CategoryStatus.ACTIVE;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == CategoryStatus.ACTIVE;
    }

    public void markActive() {
        this.status = CategoryStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = CategoryStatus.INACTIVE;
        setActive(false);
    }
}
