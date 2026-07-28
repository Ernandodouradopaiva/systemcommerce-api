package br.com.systemcommerce.finance.reconciliation.entity;

import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bank_reconciliation_rules")
public class BankReconciliationRule extends AuditableEntity {
    public enum Status { ACTIVE, INACTIVE }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "holder_id") private FinancialAccountHolder holder;
    @Column(nullable = false, length = 40) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false) private Integer priority = 100;
    @Column(name = "match_by_amount", nullable = false) private Boolean matchByAmount = true;
    @Column(name = "match_by_date", nullable = false) private Boolean matchByDate = true;
    @Column(name = "date_tolerance_days", nullable = false) private Integer dateToleranceDays = 2;
    @Column(name = "match_by_document", nullable = false) private Boolean matchByDocument = false;
    @Column(name = "description_contains", length = 200) private String descriptionContains;
    @Column(name = "auto_confirm", nullable = false) private Boolean autoConfirm = false;
    @Column(name = "safe_auto", nullable = false) private Boolean safeAuto = true;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.ACTIVE;
}
