package br.com.systemcommerce.salesorder.entity;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.quote.entity.Quote;
import br.com.systemcommerce.sale.entity.Sale;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sales_orders")
public class SalesOrder extends AuditableEntity {

    public enum SalesOrderStatus {
        DRAFT,
        PENDING_APPROVAL,
        APPROVED,
        PICKING,
        PICKED,
        INVOICED,
        DELIVERED,
        CANCELLED
    }

    private static final Set<SalesOrderStatus> EDITABLE =
            EnumSet.of(SalesOrderStatus.DRAFT, SalesOrderStatus.PENDING_APPROVAL);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "order_number", nullable = false, unique = true, length = 40, updatable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id")
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** Snapshot do cliente no momento da associação — edições futuras do cadastro não alteram o histórico. */
    @Column(name = "customer_name_snapshot", length = 200)
    private String customerNameSnapshot;

    @Column(name = "customer_document_snapshot", length = 20)
    private String customerDocumentSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    @Column(name = "carrier_name", length = 200)
    private String carrierName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SalesOrderStatus status = SalesOrderStatus.DRAFT;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "reserve_stock", nullable = false)
    private Boolean reserveStock = Boolean.FALSE;

    @Column(name = "subtotal_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "freight_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal freightAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_sale_id")
    private Sale generatedSale;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<SalesOrderItem> items = new ArrayList<>();

    public boolean isEditable() {
        return EDITABLE.contains(status);
    }

    public boolean hasGeneratedSale() {
        return generatedSale != null;
    }

    public void addItem(SalesOrderItem item) {
        items.add(item);
        item.setSalesOrder(this);
    }

    public void clearItems() {
        items.forEach(i -> i.setSalesOrder(null));
        items.clear();
    }
}
