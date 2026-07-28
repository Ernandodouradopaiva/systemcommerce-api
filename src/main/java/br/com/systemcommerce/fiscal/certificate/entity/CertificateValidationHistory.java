package br.com.systemcommerce.fiscal.certificate.entity;

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
@Table(name = "certificate_validation_history")
public class CertificateValidationHistory {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificate_id", nullable = false)
    private DigitalCertificate certificate;

    @Column(name = "validated_at", nullable = false)
    private Instant validatedAt = Instant.now();

    @Column(name = "validated_by")
    private UUID validatedBy;

    @Column(name = "result", length = 40)
    private String result;

    @Column(name = "message", length = 2000)
    private String message;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (validatedAt == null) {
            validatedAt = Instant.now();
        }
    }
}
