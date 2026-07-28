package br.com.systemcommerce.uom.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "units_of_measure")
public class UnitOfMeasure extends AuditableEntity {

    public enum UomStatus {
        ACTIVE,
        INACTIVE
    }

    /** Nulo = unidade global do sistema, disponível para todas as organizações. */
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "symbol", length = 20)
    private String symbol;

    @Column(name = "precision_scale", nullable = false)
    private Integer precisionScale = 4;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UomStatus status = UomStatus.ACTIVE;

    @Column(name = "system_unit", nullable = false)
    private Boolean systemUnit = Boolean.FALSE;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == UomStatus.ACTIVE;
    }

    public void markActive() {
        this.status = UomStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = UomStatus.INACTIVE;
        setActive(false);
    }
}
