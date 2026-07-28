package br.com.systemcommerce.picking.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.reservation.entity.StockReservation;
import br.com.systemcommerce.salesorder.entity.SalesOrder;
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

/** Separação/picking (Prompt 71) — nunca baixa estoque físico, apenas organiza a separação dos itens. */
@Getter
@Setter
@Entity
@Table(name = "picking_orders")
public class PickingOrder extends AuditableEntity {

    public enum PickingOrderStatus {
        PENDING,
        ASSIGNED,
        IN_PROGRESS,
        PARTIALLY_PICKED,
        PICKED,
        DIVERGENT,
        CANCELLED
    }

    private static final Set<PickingOrderStatus> OPEN =
            EnumSet.of(
                    PickingOrderStatus.PENDING,
                    PickingOrderStatus.ASSIGNED,
                    PickingOrderStatus.IN_PROGRESS,
                    PickingOrderStatus.PARTIALLY_PICKED,
                    PickingOrderStatus.DIVERGENT);

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
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_reservation_id")
    private StockReservation stockReservation;

    @Column(name = "picking_number", nullable = false, unique = true, length = 40, updatable = false)
    private String pickingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PickingOrderStatus status = PickingOrderStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "notes", length = 2000)
    private String notes;

    @OneToMany(mappedBy = "pickingOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<PickingOrderItem> items = new ArrayList<>();

    public boolean isOpen() {
        return OPEN.contains(status);
    }

    public void addItem(PickingOrderItem item) {
        items.add(item);
        item.setPickingOrder(this);
    }
}
