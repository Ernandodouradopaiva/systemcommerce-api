package br.com.systemcommerce.fiscal.numbering.entity;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
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
        name = "fiscal_number_voiding_requests",
        uniqueConstraints = @UniqueConstraint(name = "uk_fnvr_idem", columnNames = {"idempotency_key"}))
public class FiscalNumberVoidingRequest extends AuditableEntity {

    public enum VoidingStatus {
        DRAFT,
        QUEUED,
        SENT,
        AUTHORIZED,
        REJECTED,
        ERROR
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private FiscalEstablishment establishment;

    @Column(name = "model", nullable = false, length = 10)
    private String model;

    @Column(name = "series", nullable = false, length = 10)
    private String series;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 20)
    private FiscalEstablishment.FiscalEnvironment environment;

    @Column(name = "from_number", nullable = false)
    private Long fromNumber;

    @Column(name = "to_number", nullable = false)
    private Long toNumber;

    @Column(name = "justification", nullable = false, length = 500)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VoidingStatus status = VoidingStatus.DRAFT;

    @Column(name = "protocol_number", length = 60)
    private String protocolNumber;

    @Column(name = "sefaz_cstat", length = 10)
    private String sefazCstat;

    @Column(name = "sefaz_xmotivo", length = 500)
    private String sefazXmotivo;

    @Column(name = "xml_event_ref", columnDefinition = "TEXT")
    private String xmlEventRef;

    @Column(name = "transmitted_at")
    private Instant transmittedAt;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;
}
