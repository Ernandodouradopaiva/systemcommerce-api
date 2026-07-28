package br.com.systemcommerce.finance.billing.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "billing_status_history")
public class BillingStatusHistory {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "billing_document_id", nullable = false)
    private BillingDocument billingDocument;
    @Column(name = "from_status", length = 30) private String fromStatus;
    @Column(name = "to_status", nullable = false, length = 30) private String toStatus;
    @Column(name = "changed_at", nullable = false) private Instant changedAt = Instant.now();
    @Column(name = "changed_by") private UUID changedBy;
    @Column(length = 1000) private String notes;
    @Column(name = "external_event_id", length = 120) private String externalEventId;
    @PrePersist void pre() { if (id == null) id = UUID.randomUUID(); if (changedAt == null) changedAt = Instant.now(); }
}
