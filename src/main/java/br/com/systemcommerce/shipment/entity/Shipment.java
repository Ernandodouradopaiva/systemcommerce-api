package br.com.systemcommerce.shipment.entity;

import br.com.systemcommerce.carrier.entity.Carrier;
import br.com.systemcommerce.carrier.entity.FreightMode;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.picking.entity.PickingOrder;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Expedição/entrega (Prompt 72). A entrega ({@link #markDelivered}) NÃO altera estoque — o estoque
 * físico já saiu na confirmação da venda (faturamento). Pode ser parcial em relação ao pedido.
 */
@Getter
@Setter
@Entity
@Table(name = "shipments")
public class Shipment extends AuditableEntity {

    public enum ShipmentStatus {
        PENDING,
        PACKING,
        READY,
        DISPATCHED,
        IN_TRANSIT,
        OUT_FOR_DELIVERY,
        DELIVERED,
        DELIVERY_FAILED,
        RETURNING,
        RETURNED,
        CANCELLED
    }

    private static final Set<ShipmentStatus> OPEN = EnumSet.of(
            ShipmentStatus.PENDING,
            ShipmentStatus.PACKING,
            ShipmentStatus.READY,
            ShipmentStatus.DISPATCHED,
            ShipmentStatus.IN_TRANSIT,
            ShipmentStatus.OUT_FOR_DELIVERY,
            ShipmentStatus.DELIVERY_FAILED,
            ShipmentStatus.RETURNING);

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
    @JoinColumn(name = "picking_order_id")
    private PickingOrder pickingOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "shipment_number", nullable = false, unique = true, length = 40, updatable = false)
    private String shipmentNumber;

    @Column(name = "carrier_name", length = 200)
    private String carrierName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id")
    private Carrier carrier;

    @Column(name = "freight_mode", length = 40)
    private String freightModeLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freight_mode_id")
    private FreightMode freightMode;

    @Column(name = "freight_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal freightAmount = BigDecimal.ZERO;

    @Column(name = "tracking_code", length = 80)
    private String trackingCode;

    @Column(name = "package_count", nullable = false)
    private Integer packageCount = 1;

    @Column(name = "total_weight", precision = 18, scale = 4)
    private BigDecimal totalWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ShipmentStatus status = ShipmentStatus.PENDING;

    @Column(name = "expected_delivery")
    private LocalDate expectedDelivery;

    /** Snapshot JSON do endereço do cliente no momento da expedição (histórico imutável). */
    @Column(name = "address_snapshot", columnDefinition = "TEXT")
    private String addressSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private User responsibleUser;

    @Column(name = "notes", length = 2000)
    private String notes;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<ShipmentItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("packageNumber ASC")
    private List<ShipmentPackage> packages = new ArrayList<>();

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("occurredAt ASC")
    private List<ShipmentTracking> trackingEvents = new ArrayList<>();

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("occurredAt ASC")
    private List<DeliveryEvent> deliveryEvents = new ArrayList<>();

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("capturedAt ASC")
    private List<DeliveryProof> deliveryProofs = new ArrayList<>();

    public boolean isOpen() {
        return OPEN.contains(status);
    }

    public boolean isDelivered() {
        return status == ShipmentStatus.DELIVERED;
    }

    public void addItem(ShipmentItem item) {
        items.add(item);
        item.setShipment(this);
    }

    public void markDelivered() {
        this.status = ShipmentStatus.DELIVERED;
    }
}
