package br.com.systemcommerce.purchase.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import br.com.systemcommerce.supplier.entity.Supplier;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/** Devolução ao fornecedor (Prompt 63). Só movimenta estoque ao ser concluída (COMPLETED). */
@Getter
@Setter
@Entity
@Table(name = "supplier_returns")
public class SupplierReturn extends AuditableEntity {

    public enum ReturnReason {
        WRONG_PRODUCT,
        EXCESS_QUANTITY,
        DAMAGE,
        DEFECT,
        EXPIRY,
        DIVERGENCE,
        CANCELLATION,
        OTHER
    }

    public enum SupplierReturnStatus {
        DRAFT,
        PENDING_APPROVAL,
        APPROVED,
        DISPATCHED,
        COMPLETED,
        REJECTED,
        CANCELLED
    }

    public enum OriginType {
        RECEIPT,
        PURCHASE_ORDER,
        BATCH,
        INSPECTION,
        EXISTING_STOCK
    }

    private static final Set<SupplierReturnStatus> EDITABLE = EnumSet.of(SupplierReturnStatus.DRAFT);

    private static final Set<SupplierReturnStatus> CANCELLABLE =
            EnumSet.of(SupplierReturnStatus.DRAFT, SupplierReturnStatus.PENDING_APPROVAL, SupplierReturnStatus.APPROVED);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_receipt_id")
    private PurchaseReceipt purchaseReceipt;

    @Column(name = "return_number", nullable = false, unique = true, length = 40, updatable = false)
    private String returnNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 40)
    private ReturnReason reason;

    @Column(name = "reason_notes", length = 2000)
    private String reasonNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SupplierReturnStatus status = SupplierReturnStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", nullable = false, length = 40)
    private OriginType originType;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "notes", length = 2000)
    private String notes;

    @OneToMany(mappedBy = "supplierReturn", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<SupplierReturnItem> items = new ArrayList<>();

    public void addItem(SupplierReturnItem item) {
        items.add(item);
        item.setSupplierReturn(this);
    }

    public void clearItems() {
        items.forEach(i -> i.setSupplierReturn(null));
        items.clear();
    }

    public boolean isEditable() {
        return EDITABLE.contains(status);
    }

    public boolean isCancellable() {
        return CANCELLABLE.contains(status);
    }
}
