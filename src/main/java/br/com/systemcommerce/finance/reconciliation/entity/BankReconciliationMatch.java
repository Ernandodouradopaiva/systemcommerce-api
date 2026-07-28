package br.com.systemcommerce.finance.reconciliation.entity;

import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bank_reconciliation_matches")
public class BankReconciliationMatch extends AuditableEntity {
    public enum MatchStatus { SUGGESTED, CONFIRMED, UNDONE, IGNORED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reconciliation_id", nullable = false)
    private BankReconciliation reconciliation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "statement_entry_id", nullable = false)
    private BankStatementEntry statementEntry;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "holder_movement_id") private FinancialHolderMovement holderMovement;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rule_id") private BankReconciliationRule rule;
    @Enumerated(EnumType.STRING) @Column(name = "match_status", nullable = false, length = 30) private MatchStatus matchStatus;
    @Column(name = "matched_amount", nullable = false, precision = 18, scale = 2) private BigDecimal matchedAmount;
    @Column(name = "divergence_amount", nullable = false, precision = 18, scale = 2) private BigDecimal divergenceAmount = BigDecimal.ZERO;
    @Column(nullable = false) private Boolean suggested = false;
    @Column(name = "confirmed_at") private Instant confirmedAt;
    @Column(name = "confirmed_by") private UUID confirmedBy;
    @Column(name = "undone_at") private Instant undoneAt;
    @Column(name = "undone_by") private UUID undoneBy;
    @Column(length = 1000) private String notes;
}
