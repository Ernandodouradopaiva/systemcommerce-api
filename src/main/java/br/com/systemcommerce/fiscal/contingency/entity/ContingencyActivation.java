package br.com.systemcommerce.fiscal.contingency.entity;

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
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_contingency_activations")
public class ContingencyActivation {

    public enum TriggerKind {
        NETWORK,
        SERVICE_DOWN,
        MANUAL
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contingency_id", nullable = false)
    private FiscalContingency contingency;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_kind", nullable = false, length = 30)
    private TriggerKind triggerKind;

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (activatedAt == null) {
            activatedAt = Instant.now();
        }
    }
}
