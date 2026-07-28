package br.com.systemcommerce.finance.reconciliation.entity;

import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bank_reconciliations")
public class BankReconciliation extends AuditableEntity {
    public enum Status { OPEN, COMPLETED, CANCELLED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "holder_id", nullable = false)
    private FinancialAccountHolder holder;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "statement_id") private BankStatement statement;
    @Column(name = "reconciliation_date", nullable = false) private LocalDate reconciliationDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.OPEN;
    @Column(length = 2000) private String notes;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @OneToMany(mappedBy = "reconciliation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BankReconciliationMatch> matches = new ArrayList<>();
}
