package br.com.systemcommerce.fiscal.event.cce.entity;

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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "fiscal_correction_letters",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_fcl_doc_seq", columnNames = {"document_id", "sequence"}),
            @UniqueConstraint(name = "uk_fcl_idem", columnNames = {"idempotency_key"})
        })
public class CorrectionLetter extends AuditableEntity {

    public enum Status {
        DRAFT,
        QUEUED,
        SENT,
        AUTHORIZED,
        REJECTED,
        ERROR
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private FiscalDocument document;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Column(name = "correction_text", nullable = false, columnDefinition = "TEXT")
    private String correctionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(name = "protocol_number", length = 60)
    private String protocolNumber;

    @Column(name = "sefaz_cstat", length = 10)
    private String sefazCstat;

    @Column(name = "sefaz_xmotivo", length = 500)
    private String sefazXmotivo;

    @Column(name = "transmitted_at")
    private Instant transmittedAt;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "validation_warnings", columnDefinition = "TEXT")
    private String validationWarnings;
}
