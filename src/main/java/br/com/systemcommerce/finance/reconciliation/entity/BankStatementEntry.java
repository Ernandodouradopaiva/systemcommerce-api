package br.com.systemcommerce.finance.reconciliation.entity;

import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bank_statement_entries")
public class BankStatementEntry extends AuditableEntity {
    public enum EntryType { CREDIT, DEBIT }
    public enum ReconciliationStatus { UNMATCHED, SUGGESTED, MATCHED, PARTIALLY_MATCHED, IGNORED, DIVERGENT }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "statement_id", nullable = false)
    private BankStatement statement;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "holder_id", nullable = false)
    private FinancialAccountHolder holder;
    @Column(name = "entry_date", nullable = false) private LocalDate entryDate;
    @Column(nullable = false, length = 500) private String description;
    @Column(name = "document_number", length = 80) private String documentNumber;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(name = "entry_type", nullable = false, length = 20) private EntryType entryType;
    @Column(name = "external_id", length = 120) private String externalId;
    @Column(name = "informed_balance", precision = 18, scale = 2) private BigDecimal informedBalance;
    @Enumerated(EnumType.STRING) @Column(name = "reconciliation_status", nullable = false, length = 30)
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.UNMATCHED;
    @Column(name = "fit_id", length = 120) private String fitId;
    @Column(name = "raw_line", columnDefinition = "TEXT") private String rawLine;
}
