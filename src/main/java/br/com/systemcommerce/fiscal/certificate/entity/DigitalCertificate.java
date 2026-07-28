package br.com.systemcommerce.fiscal.certificate.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "digital_certificates")
public class DigitalCertificate extends AuditableEntity {

    public enum CertificateType {
        A1,
        A3
    }

    public enum CertificateStatus {
        PENDING,
        VALID,
        ACTIVE,
        EXPIRED,
        REVOKED,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private CertificateType type;

    @Column(name = "holder_name", length = 200)
    private String holderName;

    @Column(name = "cnpj", length = 14)
    private String cnpj;

    @Column(name = "issuer_name", length = 300)
    private String issuerName;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private CertificateStatus status = CertificateStatus.PENDING;

    @Column(name = "storage_ref", nullable = false, length = 100)
    private String storageRef;

    @Column(name = "thumbprint", nullable = false, length = 128)
    private String thumbprint;

    @Column(name = "encrypted_keystore", columnDefinition = "TEXT")
    private String encryptedKeystore;

    @Column(name = "encrypted_password", columnDefinition = "TEXT")
    private String encryptedPassword;

    @Column(name = "last_tested_at")
    private Instant lastTestedAt;

    @Column(name = "last_test_result", length = 40)
    private String lastTestResult;

    public boolean hasKeystore() {
        return encryptedKeystore != null && !encryptedKeystore.isBlank();
    }

    public boolean hasPasswordConfigured() {
        return encryptedPassword != null && !encryptedPassword.isBlank();
    }

    public boolean isExpired() {
        return validUntil != null && Instant.now().isAfter(validUntil);
    }
}
