package br.com.systemcommerce.finance.payable.entity;

import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentMethod;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
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
@Table(name = "payable_settlements")
public class PayableSettlement extends AuditableEntity {
    public enum Status { PENDING, SCHEDULED, CONFIRMED, REVERSED, CANCELLED, FAILED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "store_id") private Store store;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "holder_id", nullable = false)
    private FinancialAccountHolder holder;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_method_id") private PaymentMethod paymentMethod;
    @Column(name = "payment_date", nullable = false) private LocalDate paymentDate;
    @Column(name = "effective_date", nullable = false) private LocalDate effectiveDate;
    @Column(name = "principal_amount", nullable = false, precision = 18, scale = 2) private BigDecimal principalAmount;
    @Column(name = "interest_amount", nullable = false, precision = 18, scale = 2) private BigDecimal interestAmount = BigDecimal.ZERO;
    @Column(name = "fine_amount", nullable = false, precision = 18, scale = 2) private BigDecimal fineAmount = BigDecimal.ZERO;
    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(name = "fee_amount", nullable = false, precision = 18, scale = 2) private BigDecimal feeAmount = BigDecimal.ZERO;
    @Column(name = "total_disbursed", nullable = false, precision = 18, scale = 2) private BigDecimal totalDisbursed;
    @Column(name = "reference_code", length = 120) private String referenceCode;
    @Column(name = "receipt_url", length = 500) private String receiptUrl;
    @Column(length = 1000) private String notes;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.PENDING;
    @Column(name = "idempotency_key", nullable = false, length = 100) private String idempotencyKey;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "holder_movement_id") private FinancialHolderMovement holderMovement;
    @Column(name = "cancelled_reason", length = 500) private String cancelledReason;
    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayableSettlementAllocation> allocations = new ArrayList<>();
}