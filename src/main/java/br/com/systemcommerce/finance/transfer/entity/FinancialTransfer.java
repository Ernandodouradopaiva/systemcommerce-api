package br.com.systemcommerce.finance.transfer.entity;

import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.cash.entity.CashSession;
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
@Table(name = "financial_transfers")
public class FinancialTransfer extends AuditableEntity {

    public enum Status {
        DRAFT,
        CONFIRMED,
        CANCELLED,
        REVERSED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_holder_id", nullable = false)
    private FinancialAccountHolder sourceHolder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_holder_id", nullable = false)
    private FinancialAccountHolder targetHolder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_store_id")
    private Store sourceStore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_store_id")
    private Store targetStore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_session_id")
    private CashSession cashSession;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "fee_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_movement_id")
    private FinancialHolderMovement sourceMovement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_movement_id")
    private FinancialHolderMovement targetMovement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_movement_id")
    private FinancialHolderMovement feeMovement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reverse_of_id")
    private FinancialTransfer reverseOf;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(length = 2000)
    private String notes;
}
