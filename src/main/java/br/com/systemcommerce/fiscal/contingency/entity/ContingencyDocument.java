package br.com.systemcommerce.fiscal.contingency.entity;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
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
@Table(name = "fiscal_contingency_documents")
public class ContingencyDocument {

    public enum DocumentStatus {
        PENDING,
        RETRANSMITTED,
        AUTHORIZED,
        DISCARDED
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contingency_id", nullable = false)
    private FiscalContingency contingency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private FiscalDocument document;

    @Column(name = "pending_retransmission", nullable = false)
    private Boolean pendingRetransmission = Boolean.TRUE;

    @Column(name = "last_consult_at")
    private Instant lastConsultAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.PENDING;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
