package br.com.systemcommerce.pos.store.entity;

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
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stores")
public class Store extends AuditableEntity {

    public enum StoreStatus {
        ACTIVE,
        INACTIVE
    }

    public enum EstablishmentType {
        HEADQUARTERS,
        BRANCH,
        DISTRIBUTION_CENTER,
        VIRTUAL_STORE,
        OTHER
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "trade_name", length = 200)
    private String tradeName;

    @Column(name = "document", length = 20)
    private String document;

    @Column(name = "state_registration", length = 30)
    private String stateRegistration;

    @Column(name = "municipal_registration", length = 30)
    private String municipalRegistration;

    @Enumerated(EnumType.STRING)
    @Column(name = "establishment_type", nullable = false, length = 40)
    private EstablishmentType establishmentType = EstablishmentType.BRANCH;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

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

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "America/Sao_Paulo";

    @Column(name = "is_headquarters", nullable = false)
    private boolean headquarters = false;

    @Column(name = "opening_date")
    private LocalDate openingDate;

    @Column(name = "allows_sales", nullable = false)
    private boolean allowsSales = true;

    @Column(name = "allows_pos", nullable = false)
    private boolean allowsPos = true;

    @Column(name = "require_seller_admin", nullable = false)
    private boolean requireSellerAdmin = false;

    @Column(name = "require_seller_pos", nullable = false)
    private boolean requireSellerPos = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StoreStatus status = StoreStatus.ACTIVE;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == StoreStatus.ACTIVE;
    }

    public boolean canRegisterSales() {
        return isUsable() && allowsSales;
    }

    public boolean canOperatePos() {
        return isUsable() && allowsPos;
    }

    public void markActive() {
        this.status = StoreStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = StoreStatus.INACTIVE;
        setActive(false);
    }
}
