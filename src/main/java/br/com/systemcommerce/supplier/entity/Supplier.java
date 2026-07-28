package br.com.systemcommerce.supplier.entity;

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
@Table(name = "suppliers")
public class Supplier extends AuditableEntity {

    public enum PersonType {
        PF,
        PJ
    }

    public enum SupplierStatus {
        ACTIVE,
        INACTIVE,
        BLOCKED
    }

    public enum TaxContributorIndicator {
        CONTRIBUTOR,
        EXEMPT,
        NON_CONTRIBUTOR
    }

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 2)
    private PersonType type;

    @Column(name = "document", nullable = false, unique = true, length = 20)
    private String document;

    @Column(name = "state_registration", length = 30)
    private String stateRegistration;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "trade_name", length = 200)
    private String tradeName;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "mobile", length = 30)
    private String mobile;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "street", length = 200)
    private String street;

    @Column(name = "number", length = 20)
    private String number;

    @Column(name = "complement", length = 100)
    private String complement;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 2)
    private String state;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "municipal_registration", length = 30)
    private String municipalRegistration;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_contributor_indicator", length = 20)
    private TaxContributorIndicator taxContributorIndicator;

    @Column(name = "category", length = 60)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SupplierStatus status = SupplierStatus.ACTIVE;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** Fornecedor utilizável em compras: precisa estar ativo (soft-delete) e com status ACTIVE. */
    public boolean isUsableForPurchase() {
        return Boolean.TRUE.equals(getActive()) && status == SupplierStatus.ACTIVE;
    }

    public boolean isBlocked() {
        return status == SupplierStatus.BLOCKED;
    }

    public void markActive() {
        this.status = SupplierStatus.ACTIVE;
        this.blockedAt = null;
        this.blockedReason = null;
        setActive(true);
    }

    public void markInactive() {
        this.status = SupplierStatus.INACTIVE;
        setActive(false);
    }

    public void markBlocked(String reason) {
        this.status = SupplierStatus.BLOCKED;
        this.blockedAt = Instant.now();
        this.blockedReason = reason;
    }

    public void markUnblocked() {
        this.status = SupplierStatus.ACTIVE;
        this.blockedAt = null;
        this.blockedReason = null;
        setActive(true);
    }
}
