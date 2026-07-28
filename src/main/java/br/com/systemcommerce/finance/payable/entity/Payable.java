package br.com.systemcommerce.finance.payable.entity;

import br.com.systemcommerce.finance.account.entity.FinancialCategory;
import br.com.systemcommerce.finance.costcenter.entity.CostCenter;
import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentCondition;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import br.com.systemcommerce.supplier.entity.Supplier;
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
@Table(name = "payables")
public class Payable extends AuditableEntity {
    public enum Status { DRAFT, OPEN, PARTIALLY_PAID, PAID, OVERDUE, SCHEDULED, CANCELLED, RENEGOTIATED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "store_id") private Store store;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_condition_id") private PaymentCondition paymentCondition;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "financial_category_id") private FinancialCategory financialCategory;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cost_center_id") private CostCenter costCenter;
    @Column(name = "document_number", length = 60) private String documentNumber;
    @Column(name = "issue_date", nullable = false) private LocalDate issueDate;
    @Column(name = "competence_date", nullable = false) private LocalDate competenceDate;
    @Column(name = "original_amount", nullable = false, precision = 18, scale = 2) private BigDecimal originalAmount;
    @Column(name = "planned_discount", nullable = false, precision = 18, scale = 2) private BigDecimal plannedDiscount = BigDecimal.ZERO;
    @Column(name = "planned_addition", nullable = false, precision = 18, scale = 2) private BigDecimal plannedAddition = BigDecimal.ZERO;
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2) private BigDecimal totalAmount;
    @Column(name = "paid_amount", nullable = false, precision = 18, scale = 2) private BigDecimal paidAmount = BigDecimal.ZERO;
    @Column(name = "balance_amount", nullable = false, precision = 18, scale = 2) private BigDecimal balanceAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.DRAFT;
    @Column(length = 2000) private String notes;
    @Column(name = "cancel_reason", length = 500) private String cancelReason;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
    @OneToMany(mappedBy = "payable", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("installmentNumber ASC")
    private List<PayableInstallment> installments = new ArrayList<>();
    @OneToMany(mappedBy = "payable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayableOrigin> origins = new ArrayList<>();
    @OneToMany(mappedBy = "payable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayableAllocation> allocations = new ArrayList<>();

    public boolean isEditable() { return status == Status.DRAFT; }
    public boolean isPaid() { return status == Status.PAID; }
}