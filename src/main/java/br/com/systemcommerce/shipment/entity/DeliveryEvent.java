package br.com.systemcommerce.shipment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Evento interno de entrega (mudança de status, tentativa, ocorrência) — Prompt 72. */
@Getter
@Setter
@Entity
@Table(name = "delivery_events")
public class DeliveryEvent {

    public enum EventType {
        STATUS_CHANGED,
        DELIVERY_ATTEMPT,
        DELIVERY_FAILED,
        DELIVERED,
        RETURN_STARTED,
        RETURNED,
        CANCELLED
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "performed_by")
    private UUID performedBy;

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
