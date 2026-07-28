package br.com.systemcommerce.customer.entity;

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

/** Consentimento LGPD do cliente — histórico não apagável (revogação apenas marca revokedAt). */
@Getter
@Setter
@Entity
@Table(name = "customer_consents")
public class CustomerConsent extends AuditableEntity {

    public enum ConsentType {
        MARKETING,
        DATA_PROCESSING,
        OTHER
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ConsentType type;

    @Column(name = "granted", nullable = false)
    private Boolean granted;

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    public void revoke() {
        this.granted = false;
        this.revokedAt = Instant.now();
    }
}
