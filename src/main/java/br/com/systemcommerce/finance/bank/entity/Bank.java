package br.com.systemcommerce.finance.bank.entity;

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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "banks")
public class Bank extends AuditableEntity {

    public enum BankStatus {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "short_name", length = 80)
    private String shortName;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode = "BR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BankStatus status = BankStatus.ACTIVE;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == BankStatus.ACTIVE;
    }

    public void markActive() {
        status = BankStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        status = BankStatus.INACTIVE;
        setActive(false);
    }
}
