package br.com.systemcommerce.finance.card.entity;

import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.finance.reconciliation.entity.BankStatementEntry;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "card_settlements")
public class CardSettlement extends AuditableEntity {
    public enum Status { PENDING, SETTLED, CANCELLED, DIVERGENT }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "acquirer_id", nullable = false)
    private Acquirer acquirer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "holder_id", nullable = false)
    private FinancialAccountHolder holder;
    @Column(name = "settlement_date", nullable = false) private LocalDate settlementDate;
    @Column(name = "gross_amount", nullable = false, precision = 18, scale = 2) private BigDecimal grossAmount;
    @Column(name = "fee_amount", nullable = false, precision = 18, scale = 2) private BigDecimal feeAmount = BigDecimal.ZERO;
    @Column(name = "net_amount", nullable = false, precision = 18, scale = 2) private BigDecimal netAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.PENDING;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "holder_movement_id") private FinancialHolderMovement holderMovement;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "bank_statement_entry_id") private BankStatementEntry bankStatementEntry;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @Column(length = 2000) private String notes;
    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CardSettlementItem> items = new ArrayList<>();
}
