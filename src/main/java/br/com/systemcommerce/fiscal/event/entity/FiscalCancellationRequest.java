package br.com.systemcommerce.fiscal.event.entity;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "fiscal_cancellation_requests",
        uniqueConstraints = @UniqueConstraint(name = "uk_fcr_idem", columnNames = {"idempotency_key"}))
public class FiscalCancellationRequest extends AuditableEntity {

    public enum CancellationStatus {
        DRAFT,
        PENDING_APPROVAL,
        APPROVED,
        QUEUED,
        SENT,
        AUTHORIZED,
        REJECTED,
        ERROR
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private FiscalDocument document;

    @Column(name = "justification", nullable = false, length = 500)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CancellationStatus status = CancellationStatus.DRAFT;

    @Column(name = "protocol_number", length = 60)
    private String protocolNumber;

    @Column(name = "sefaz_cstat", length = 10)
    private String sefazCstat;

    @Column(name = "sefaz_xmotivo", length = 500)
    private String sefazXmotivo;

    @Column(name = "event_xml_ref", columnDefinition = "TEXT")
    private String eventXmlRef;

    @Column(name = "transmitted_at")
    private Instant transmittedAt;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "requested_by")
    private java.util.UUID requestedBy;
}
