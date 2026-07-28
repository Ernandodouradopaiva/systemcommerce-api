package br.com.systemcommerce.purchase.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Cotação de compra multifornecedor (Prompt 60). Distinta de {@code Quote} (orçamento de venda).
 */
@Getter
@Setter
@Entity
@Table(name = "purchase_quotations")
public class PurchaseQuotation extends AuditableEntity {

    public enum PurchaseQuotationStatus {
        DRAFT,
        OPEN,
        SENT,
        RESPONSES_PENDING,
        UNDER_COMPARISON,
        PARTIALLY_SELECTED,
        SELECTED,
        CLOSED,
        CANCELLED
    }

    public enum SelectionCriteria {
        TOTAL_COST,
        UNIT_PRICE,
        LEAD_TIME,
        MANUAL
    }

    private static final Set<PurchaseQuotationStatus> EDITABLE =
            EnumSet.of(PurchaseQuotationStatus.DRAFT, PurchaseQuotationStatus.OPEN);

    private static final Set<PurchaseQuotationStatus> LOCKED =
            EnumSet.of(PurchaseQuotationStatus.CLOSED, PurchaseQuotationStatus.CANCELLED);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_user_id")
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id")
    private PurchaseRequest purchaseRequest;

    @Column(name = "quotation_number", nullable = false, unique = true, length = 40, updatable = false)
    private String quotationNumber;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "response_deadline")
    private Instant responseDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PurchaseQuotationStatus status = PurchaseQuotationStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_criteria", nullable = false, length = 40)
    private SelectionCriteria selectionCriteria = SelectionCriteria.TOTAL_COST;

    @Column(name = "auto_select_lowest_price", nullable = false)
    private Boolean autoSelectLowestPrice = Boolean.FALSE;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToMany(mappedBy = "purchaseQuotation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<PurchaseQuotationItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "purchaseQuotation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("invitedAt ASC")
    private List<PurchaseQuotationSupplier> suppliers = new ArrayList<>();

    public boolean isEditable() {
        return EDITABLE.contains(status);
    }

    public boolean isLocked() {
        return LOCKED.contains(status);
    }

    public void addItem(PurchaseQuotationItem item) {
        items.add(item);
        item.setPurchaseQuotation(this);
    }

    public void addSupplier(PurchaseQuotationSupplier supplier) {
        suppliers.add(supplier);
        supplier.setPurchaseQuotation(this);
    }
}
