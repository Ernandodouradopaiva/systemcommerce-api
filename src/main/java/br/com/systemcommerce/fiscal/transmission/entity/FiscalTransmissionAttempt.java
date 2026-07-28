package br.com.systemcommerce.fiscal.transmission.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_transmission_attempts")
public class FiscalTransmissionAttempt extends AuditableEntity {

    public enum ErrorKind {
        NETWORK,
        FISCAL_REJECTION,
        TIMEOUT,
        UNKNOWN
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transmission_id", nullable = false)
    private FiscalTransmission transmission;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "request_digest", length = 64)
    private String requestDigest;

    @Column(name = "response_cstat", length = 10)
    private String responseCstat;

    @Column(name = "response_xmotivo", length = 500)
    private String responseXmotivo;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_kind", length = 30)
    private ErrorKind errorKind;
}
