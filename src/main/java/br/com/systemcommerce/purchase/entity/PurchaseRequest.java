package br.com.systemcommerce.purchase.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.shared.audit.AuditableEntity;
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
 * Solicitação interna de compra (Prompt 59). Não movimenta estoque nem financeiro:
 * é apenas o ponto de partida da cadeia de compras.
 */
@Getter
@Setter
@Entity
@Table(name = "purchase_requests")
public class PurchaseRequest extends AuditableEntity {

    public enum PurchaseRequestStatus {
        DRAFT,
        SUBMITTED,
        UNDER_ANALYSIS,
        APPROVED,
        PARTIALLY_APPROVED,
        REJECTED,
        IN_QUOTATION,
        CONVERTED,
        CANCELLED
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    private static final Set<PurchaseRequestStatus> EDITABLE = EnumSet.of(PurchaseRequestStatus.DRAFT);

    private static final Set<PurchaseRequestStatus> CANCELLABLE = EnumSet.of(
            PurchaseRequestStatus.DRAFT,
            PurchaseRequestStatus.SUBMITTED,
            PurchaseRequestStatus.UNDER_ANALYSIS,
            PurchaseRequestStatus.APPROVED,
            PurchaseRequestStatus.PARTIALLY_APPROVED,
            PurchaseRequestStatus.IN_QUOTATION);

    private static final Set<PurchaseRequestStatus> CONVERTIBLE = EnumSet.of(
            PurchaseRequestStatus.APPROVED,
            PurchaseRequestStatus.PARTIALLY_APPROVED,
            PurchaseRequestStatus.IN_QUOTATION);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "requesting_sector", length = 120)
    private String requestingSector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id")
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_user_id")
    private User buyer;

    @Column(name = "request_number", nullable = false, unique = true, length = 40, updatable = false)
    private String requestNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private Priority priority = Priority.NORMAL;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "desired_date")
    private LocalDate desiredDate;

    @Column(name = "justification", length = 2000)
    private String justification;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PurchaseRequestStatus status = PurchaseRequestStatus.DRAFT;

    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = Boolean.TRUE;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    @OneToMany(mappedBy = "purchaseRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<PurchaseRequestItem> items = new ArrayList<>();

    public boolean isEditable() {
        return EDITABLE.contains(status);
    }

    public boolean isCancellable() {
        return CANCELLABLE.contains(status);
    }

    public boolean isConvertible() {
        return CONVERTIBLE.contains(status) && items.stream().anyMatch(i -> i.pendingQuantity().signum() > 0);
    }

    public void addItem(PurchaseRequestItem item) {
        items.add(item);
        item.setPurchaseRequest(this);
    }

    public void clearItems() {
        items.forEach(i -> i.setPurchaseRequest(null));
        items.clear();
    }
}
