package br.com.systemcommerce.fiscal.transmission.entity;

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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_transmissions")
public class FiscalTransmission extends AuditableEntity {

    public enum TransmissionStatus {
        PENDING,
        IN_PROGRESS,
        SUCCESS,
        REJECTED,
        ERROR,
        TIMEOUT
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private FiscalDocument document;

    @Column(name = "operation", nullable = false, length = 40)
    private String operation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransmissionStatus status = TransmissionStatus.PENDING;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;
}
