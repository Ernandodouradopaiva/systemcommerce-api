package br.com.systemcommerce.fiscal.certificate.entity;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "certificate_usage_logs")
public class CertificateUsageLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificate_id", nullable = false)
    private DigitalCertificate certificate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "establishment_id")
    private FiscalEstablishment establishment;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt = Instant.now();

    @Column(name = "purpose", length = 80)
    private String purpose;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "performed_by")
    private UUID performedBy;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (usedAt == null) {
            usedAt = Instant.now();
        }
    }
}
