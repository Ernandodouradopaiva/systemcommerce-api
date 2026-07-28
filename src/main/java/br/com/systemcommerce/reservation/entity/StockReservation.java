package br.com.systemcommerce.reservation.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.shared.audit.AuditableEntity;
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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Reserva formal de estoque (Prompt 70) — reduz a quantidade disponível (via {@code quantityReserved} do
 * {@code Inventory}), nunca o saldo físico. Fonte da verdade de disponibilidade fica na API.
 */
@Getter
@Setter
@Entity
@Table(name = "stock_reservations")
public class StockReservation extends AuditableEntity {

    public enum OriginType {
        QUOTE,
        SALES_ORDER,
        MARKETPLACE,
        ONLINE,
        SPECIAL_SERVICE
    }

    public enum ReservationStatus {
        ACTIVE,
        PARTIALLY_CONSUMED,
        CONSUMED,
        RELEASED,
        EXPIRED,
        CANCELLED
    }

    private static final Set<ReservationStatus> OPEN =
            EnumSet.of(ReservationStatus.ACTIVE, ReservationStatus.PARTIALLY_CONSUMED);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "reservation_number", nullable = false, unique = true, length = 40, updatable = false)
    private String reservationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", nullable = false, length = 40)
    private OriginType originType;

    @Column(name = "origin_id", nullable = false)
    private UUID originId;

    @Column(name = "origin_number", length = 40)
    private String originNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "idempotency_key", length = 80)
    private String idempotencyKey;

    @OneToMany(mappedBy = "stockReservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<StockReservationItem> items = new ArrayList<>();

    public boolean isOpen() {
        return OPEN.contains(status);
    }

    public void addItem(StockReservationItem item) {
        items.add(item);
        item.setStockReservation(this);
    }
}
