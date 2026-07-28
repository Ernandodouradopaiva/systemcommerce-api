package br.com.systemcommerce.purchase.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder extends AuditableEntity {

    /** Espelha o CHECK de {@code purchase_orders.status} (V180 + V193 — Prompt 61). */
    public enum PurchaseOrderStatus {
        DRAFT,
        PENDING_APPROVAL,
        APPROVED,
        SENT,
        SENT_TO_SUPPLIER,
        CONFIRMED_BY_SUPPLIER,
        PARTIAL,
        PARTIALLY_RECEIVED,
        RECEIVED,
        CLOSED,
        REJECTED,
        CANCELLED
    }

    private static final Set<PurchaseOrderStatus> EDITABLE = EnumSet.of(PurchaseOrderStatus.DRAFT);

    private static final Set<PurchaseOrderStatus> REVISABLE = EnumSet.of(
            PurchaseOrderStatus.SENT,
            PurchaseOrderStatus.SENT_TO_SUPPLIER,
            PurchaseOrderStatus.PENDING_APPROVAL,
            PurchaseOrderStatus.CONFIRMED_BY_SUPPLIER);

    private static final Set<PurchaseOrderStatus> TERMINAL_FOR_CANCEL =
            EnumSet.of(PurchaseOrderStatus.CANCELLED, PurchaseOrderStatus.RECEIVED, PurchaseOrderStatus.CLOSED);

    private static final Set<PurchaseOrderStatus> APPROVABLE_FROM =
            EnumSet.of(PurchaseOrderStatus.SENT, PurchaseOrderStatus.PENDING_APPROVAL);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_store_id")
    private Store destinationStore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_quotation_id")
    private PurchaseQuotation purchaseQuotation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_user_id")
    private User buyer;

    @Column(name = "order_number", nullable = false, unique = true, length = 40, updatable = false)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "payment_condition", length = 200)
    private String paymentCondition;

    @Column(name = "carrier_name", length = 200)
    private String carrierName;

    @Column(name = "freight_modality", length = 40)
    private String freightModality;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "subtotal_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "freight_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal freightAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "insurance_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal insuranceAmount = BigDecimal.ZERO;

    @Column(name = "expense_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal expenseAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber = 1;

    @Column(name = "approval_required", nullable = false)
    private Boolean approvalRequired = Boolean.FALSE;

    @Column(name = "approval_threshold_amount", precision = 18, scale = 2)
    private BigDecimal approvalThresholdAmount;

    @Column(name = "allow_over_receipt", nullable = false)
    private Boolean allowOverReceipt = Boolean.FALSE;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<PurchaseOrderItem> items = new ArrayList<>();

    public boolean isEditable() {
        return EDITABLE.contains(status);
    }

    public boolean isRevisable() {
        return REVISABLE.contains(status) && !hasAnyReceived();
    }

    public boolean isCancellable() {
        return !TERMINAL_FOR_CANCEL.contains(status) && !hasAnyReceived();
    }

    public boolean isApprovableNow() {
        return APPROVABLE_FROM.contains(status);
    }

    public boolean hasAnyReceived() {
        return items.stream()
                .anyMatch(i -> i.getQuantityReceived() != null
                        && i.getQuantityReceived().compareTo(BigDecimal.ZERO) > 0);
    }

    public void addItem(PurchaseOrderItem item) {
        items.add(item);
        item.setPurchaseOrder(this);
    }

    public void clearItems() {
        items.forEach(i -> i.setPurchaseOrder(null));
        items.clear();
    }
}
