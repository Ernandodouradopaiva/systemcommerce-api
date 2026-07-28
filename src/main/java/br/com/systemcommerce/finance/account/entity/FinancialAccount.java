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

/** Conta do plano de contas (Prompt 92). Distinta de FinancialAccountHolder (Prompt 94). */
@Getter
@Setter
@Entity
@Table(name = "financial_accounts")
public class FinancialAccount extends AuditableEntity {

    public enum AccountType {
        REVENUE,
        EXPENSE,
        ASSET,
        LIABILITY,
        TRANSFER,
        ADJUSTMENT
    }

    public enum Nature {
        CREDIT,
        DEBIT,
        NEUTRAL
    }

    public enum AccountStatus {
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
    @JoinColumn(name = "parent_id")
    private FinancialAccount parent;

    @Column(name = "level_no", nullable = false)
    private Integer levelNo = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "nature", nullable = false, length = 20)
    private Nature nature;

    @Column(name = "accepts_posting", nullable = false)
    private Boolean acceptsPosting = Boolean.FALSE;

    @Column(name = "requires_cost_center", nullable = false)
    private Boolean requiresCostCenter = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == AccountStatus.ACTIVE;
    }

    public void markActive() {
        this.status = AccountStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = AccountStatus.INACTIVE;
        setActive(false);
    }

    public boolean isSynthetic() {
        return !Boolean.TRUE.equals(acceptsPosting);
    }
}
