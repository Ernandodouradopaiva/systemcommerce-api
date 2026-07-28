package br.com.systemcommerce.finance.entry.entity;

import br.com.systemcommerce.finance.account.entity.FinancialCategory;
import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.finance.costcenter.entity.CostCenter;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_entries")
public class FinancialEntry extends AuditableEntity {

    public enum EntryType {
        MANUAL_REVENUE,
        MANUAL_EXPENSE,
        ADJUSTMENT,
        FEE,
        YIELD,
        TAX,
        CORRECTION,
        OPENING_BALANCE
    }

    public enum Status {
        DRAFT,
        CONFIRMED,
        CANCELLED,
        REVERSED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "holder_id", nullable = false)
    private FinancialAccountHolder holder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financial_category_id", nullable = false)
    private FinancialCategory financialCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 40)
    private EntryType entryType;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "competence_date", nullable = false)
    private LocalDate competenceDate;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holder_movement_id")
    private FinancialHolderMovement holderMovement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reverse_of_id")
    private FinancialEntry reverseOf;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(length = 2000)
    private String notes;
}
