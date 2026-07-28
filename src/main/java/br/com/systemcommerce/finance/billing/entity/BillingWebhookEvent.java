package br.com.systemcommerce.finance.billing.entity;

import br.com.systemcommerce.organization.entity.Organization;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "billing_webhook_events")
public class BillingWebhookEvent {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(name = "provider_code", nullable = false, length = 60) private String providerCode;
    @Column(name = "event_id", nullable = false, length = 120) private String eventId;
    @Column(name = "event_type", length = 80) private String eventType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "billing_document_id") private BillingDocument billingDocument;
    @Column(nullable = false, columnDefinition = "TEXT") private String payload;
    @Column(nullable = false) private Boolean processed = false;
    @Column(name = "processed_at") private Instant processedAt;
    @Column(name = "error_message", length = 2000) private String errorMessage;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @PrePersist void pre() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
