package br.com.systemcommerce.finance.advance.entity;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
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
@Table(name = "customer_advances")
public class CustomerAdvance extends AuditableEntity {
    public enum Status { OPEN, PARTIALLY_APPLIED, APPLIED, REFUNDED, CANCELLED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "store_id") private Store store;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "holder_id", nullable = false)
    private FinancialAccountHolder holder;
    @Column(name = "document_number", length = 60) private String documentNumber;
    @Column(name = "advance_date", nullable = false) private LocalDate advanceDate;
    @Column(name = "original_amount", nullable = false, precision = 18, scale = 2) private BigDecimal originalAmount;
    @Column(name = "applied_amount", nullable = false, precision = 18, scale = 2) private BigDecimal appliedAmount = BigDecimal.ZERO;
    @Column(name = "refunded_amount", nullable = false, precision = 18, scale = 2) private BigDecimal refundedAmount = BigDecimal.ZERO;
    @Column(name = "balance_amount", nullable = false, precision = 18, scale = 2) private BigDecimal balanceAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.OPEN;
    @Column(length = 2000) private String notes;
    @Column(name = "cancel_reason", length = 500) private String cancelReason;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "holder_movement_id") private FinancialHolderMovement holderMovement;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;

    public void refreshStatus() {
        if (status == Status.CANCELLED || status == Status.REFUNDED) return;
        if (balanceAmount.compareTo(BigDecimal.ZERO) == 0 && appliedAmount.compareTo(originalAmount) >= 0) {
            status = Status.APPLIED;
        } else if (appliedAmount.compareTo(BigDecimal.ZERO) > 0) {
            status = Status.PARTIALLY_APPLIED;
        } else {
            status = Status.OPEN;
        }
    }
}
