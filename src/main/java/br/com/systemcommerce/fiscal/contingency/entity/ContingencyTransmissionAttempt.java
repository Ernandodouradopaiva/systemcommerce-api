package br.com.systemcommerce.fiscal.contingency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_contingency_transmission_attempts")
public class ContingencyTransmissionAttempt {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contingency_document_id", nullable = false)
    private ContingencyDocument contingencyDocument;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "result", length = 40)
    private String result;

    @Column(name = "cstat", length = 10)
    private String cstat;

    @Column(name = "xmotivo", length = 500)
    private String xmotivo;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
