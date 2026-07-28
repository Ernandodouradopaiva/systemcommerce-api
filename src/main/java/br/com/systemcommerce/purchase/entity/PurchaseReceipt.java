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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Recebimento físico de mercadorias / GoodsReceipt (Prompt 62).
 * Fluxo profissional: DRAFT → UNDER_INSPECTION → (PARTIALLY_ACCEPTED|ACCEPTED) → POSTED_TO_INVENTORY.
 * {@code CONFIRMED} é status legado migrado para {@code POSTED_TO_INVENTORY} (V194).
 */
@Getter
@Setter
@Entity
@Table(name = "purchase_receipts")
public class PurchaseReceipt extends AuditableEntity {

    public enum PurchaseReceiptStatus {
        DRAFT,
        UNDER_INSPECTION,
        PARTIALLY_ACCEPTED,
        ACCEPTED,
        POSTED_TO_INVENTORY,
        CONFIRMED,
        REJECTED,
        CANCELLED
    }

    private static final Set<PurchaseReceiptStatus> INSPECTABLE =
            EnumSet.of(PurchaseReceiptStatus.DRAFT, PurchaseReceiptStatus.UNDER_INSPECTION);

    private static final Set<PurchaseReceiptStatus> ACCEPTABLE =
            EnumSet.of(PurchaseReceiptStatus.DRAFT, PurchaseReceiptStatus.UNDER_INSPECTION);

    private static final Set<PurchaseReceiptStatus> POSTABLE =
            EnumSet.of(PurchaseReceiptStatus.ACCEPTED, PurchaseReceiptStatus.PARTIALLY_ACCEPTED);

    private static final Set<PurchaseReceiptStatus> CANCELLABLE = EnumSet.of(
            PurchaseReceiptStatus.DRAFT,
            PurchaseReceiptStatus.UNDER_INSPECTION,
            PurchaseReceiptStatus.ACCEPTED,
            PurchaseReceiptStatus.PARTIALLY_ACCEPTED);

    private static final Set<PurchaseReceiptStatus> POSTED =
            EnumSet.of(PurchaseReceiptStatus.POSTED_TO_INVENTORY, PurchaseReceiptStatus.CONFIRMED);

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
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 40, updatable = false)
    private String receiptNumber;

    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Column(name = "invoice_number", length = 80)
    private String invoiceNumber;

    @Column(name = "invoice_series", length = 20)
    private String invoiceSeries;

    @Column(name = "access_key", length = 60)
    private String accessKey;

    @Column(name = "invoice_issued_at")
    private LocalDate invoiceIssuedAt;

    @Column(name = "carrier_name", length = 200)
    private String carrierName;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PurchaseReceiptStatus status = PurchaseReceiptStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by_user_id")
    private User receivedBy;

    @Column(name = "posted_at")
    private Instant postedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by")
    private User postedBy;

    @Column(name = "idempotency_key", length = 80)
    private String idempotencyKey;

    @OneToMany(mappedBy = "purchaseReceipt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<PurchaseReceiptItem> items = new ArrayList<>();

    public void addItem(PurchaseReceiptItem item) {
        items.add(item);
        item.setPurchaseReceipt(this);
    }

    public boolean isInspectable() {
        return INSPECTABLE.contains(status);
    }

    public boolean isAcceptable() {
        return ACCEPTABLE.contains(status);
    }

    public boolean isPostable() {
        return POSTABLE.contains(status);
    }

    public boolean isCancellable() {
        return CANCELLABLE.contains(status);
    }

    public boolean isPosted() {
        return POSTED.contains(status);
    }
}
