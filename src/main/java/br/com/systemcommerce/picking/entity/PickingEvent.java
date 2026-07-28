package br.com.systemcommerce.picking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "picking_events")
public class PickingEvent {

    public enum PickingEventType {
        ASSIGNED,
        STARTED,
        ITEM_PICKED,
        SUBSTITUTION,
        DIVERGENCE,
        COMPLETED,
        CANCELLED
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "picking_order_id", nullable = false)
    private PickingOrder pickingOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "picking_item_id")
    private PickingOrderItem pickingItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private PickingEventType eventType;

    @Column(name = "quantity", precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "barcode", length = 80)
    private String barcode;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "performed_by")
    private UUID performedBy;

    @Column(name = "idempotency_key", length = 80)
    private String idempotencyKey;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
