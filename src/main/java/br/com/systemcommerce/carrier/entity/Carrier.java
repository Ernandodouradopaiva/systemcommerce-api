package br.com.systemcommerce.carrier.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Transportadora (Prompt 73). Inativa não pode ser selecionada em novas expedições/pedidos. */
@Getter
@Setter
@Entity
@Table(name = "carriers")
public class Carrier extends AuditableEntity {

    public enum CarrierStatus {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "trade_name", length = 200)
    private String tradeName;

    @Column(name = "document", nullable = false, length = 20)
    private String document;

    @Column(name = "state_registration", length = 30)
    private String stateRegistration;

    @Column(name = "antt_rntrc", length = 40)
    private String anttRntrc;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CarrierStatus status = CarrierStatus.ACTIVE;

    @Column(name = "notes", length = 2000)
    private String notes;

    @OneToMany(mappedBy = "carrier", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CarrierContact> contacts = new ArrayList<>();

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == CarrierStatus.ACTIVE;
    }

    public void markActive() {
        this.status = CarrierStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = CarrierStatus.INACTIVE;
        setActive(false);
    }
}
