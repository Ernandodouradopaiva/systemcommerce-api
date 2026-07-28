package br.com.systemcommerce.fiscal.establishment.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
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
@Table(name = "fiscal_establishments")
public class FiscalEstablishment extends AuditableEntity {

    public enum FiscalEnvironment {
        HOMOLOGATION,
        PRODUCTION
    }

    public enum EstablishmentStatus {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    private Store store;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "trade_name", length = 200)
    private String tradeName;

    @Column(name = "cnpj", nullable = false, length = 14)
    private String cnpj;

    @Column(name = "state_registration", length = 30)
    private String stateRegistration;

    @Column(name = "municipal_registration", length = 30)
    private String municipalRegistration;

    @Column(name = "cnae_principal", length = 10)
    private String cnaePrincipal;

    @Column(name = "ibge_city_code", nullable = false, length = 7)
    private String ibgeCityCode;

    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    @Column(name = "zip_code", length = 8)
    private String zipCode;

    @Column(name = "street", length = 200)
    private String street;

    @Column(name = "number", length = 20)
    private String number;

    @Column(name = "complement", length = 100)
    private String complement;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "tax_regime", nullable = false, length = 40)
    private String taxRegime;

    @Column(name = "crt", nullable = false)
    private Short crt;

    @Column(name = "taxpayer_indicator", nullable = false, length = 40)
    private String taxpayerIndicator;

    @Enumerated(EnumType.STRING)
    @Column(name = "fiscal_environment", nullable = false, length = 20)
    private FiscalEnvironment fiscalEnvironment = FiscalEnvironment.HOMOLOGATION;

    @Column(name = "default_nfe_series", length = 10)
    private String defaultNfeSeries;

    @Column(name = "default_nfce_series", length = 10)
    private String defaultNfceSeries;

    @Column(name = "allows_nfe", nullable = false)
    private Boolean allowsNfe = Boolean.TRUE;

    @Column(name = "allows_nfce", nullable = false)
    private Boolean allowsNfce = Boolean.TRUE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EstablishmentStatus status = EstablishmentStatus.ACTIVE;

    @Column(name = "accreditation_date")
    private LocalDate accreditationDate;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == EstablishmentStatus.ACTIVE;
    }

    public boolean isAvailableForEmission() {
        return isUsable();
    }

    public void markActive() {
        this.status = EstablishmentStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = EstablishmentStatus.INACTIVE;
        setActive(false);
    }
}
