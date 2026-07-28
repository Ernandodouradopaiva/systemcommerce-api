package br.com.systemcommerce.organization.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "organizations")
public class Organization extends AuditableEntity {

    public enum OrganizationStatus {
        ACTIVE,
        INACTIVE
    }

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "trade_name", length = 200)
    private String tradeName;

    @Column(name = "document", length = 20)
    private String document;

    @Column(name = "state_registration", length = 30)
    private String stateRegistration;

    @Column(name = "municipal_registration", length = 30)
    private String municipalRegistration;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

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

    @Column(name = "default_timezone", nullable = false, length = 64)
    private String defaultTimezone = "America/Sao_Paulo";

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BRL";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == OrganizationStatus.ACTIVE;
    }

    public void markActive() {
        this.status = OrganizationStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = OrganizationStatus.INACTIVE;
        setActive(false);
    }
}
