package br.com.systemcommerce.fiscal.party.entity;

import br.com.systemcommerce.fiscal.party.PartyType;
import br.com.systemcommerce.fiscal.party.TaxpayerIndicator;
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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "party_fiscal_profiles")
public class PartyFiscalProfile extends AuditableEntity {

    public enum ProfileStatus {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false, length = 20)
    private PartyType partyType;

    @Column(name = "party_id", nullable = false)
    private UUID partyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "taxpayer_indicator", nullable = false, length = 40)
    private TaxpayerIndicator taxpayerIndicator;

    @Column(name = "state_registration", length = 30)
    private String stateRegistration;

    @Column(name = "municipal_registration", length = 30)
    private String municipalRegistration;

    @Column(name = "suframa", length = 20)
    private String suframa;

    @Column(name = "final_consumer", nullable = false)
    private Boolean finalConsumer = Boolean.FALSE;

    @Column(name = "rural_producer", nullable = false)
    private Boolean ruralProducer = Boolean.FALSE;

    @Column(name = "foreign_party", nullable = false)
    private Boolean foreignParty = Boolean.FALSE;

    @Column(name = "country_code", length = 10)
    private String countryCode = "1058";

    @Column(name = "ibge_city_code", length = 7)
    private String ibgeCityCode;

    @Column(name = "fiscal_email", length = 200)
    private String fiscalEmail;

    @Column(name = "tax_regime", length = 40)
    private String taxRegime;

    @Column(name = "retention_flags_json", columnDefinition = "TEXT")
    private String retentionFlagsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ProfileStatus status = ProfileStatus.ACTIVE;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == ProfileStatus.ACTIVE;
    }

    public boolean isValidOn(LocalDate date) {
        if (!isUsable()) {
            return false;
        }
        if (date == null) {
            date = LocalDate.now();
        }
        if (validFrom != null && date.isBefore(validFrom)) {
            return false;
        }
        return validUntil == null || !date.isAfter(validUntil);
    }

    public void markActive() {
        this.status = ProfileStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = ProfileStatus.INACTIVE;
        setActive(false);
    }
}
