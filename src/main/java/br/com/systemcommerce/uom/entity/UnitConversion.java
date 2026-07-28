package br.com.systemcommerce.uom.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "unit_conversions")
public class UnitConversion extends AuditableEntity {

    @Column(name = "organization_id")
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_unit_id", nullable = false)
    private UnitOfMeasure fromUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_unit_id", nullable = false)
    private UnitOfMeasure toUnit;

    /** Quantidade de {@code toUnit} equivalente a 1 {@code fromUnit}. Ex.: CX -> UN, factor = 12. */
    @Column(name = "factor", nullable = false, precision = 24, scale = 10)
    private BigDecimal factor;

    @Enumerated(EnumType.STRING)
    @Column(name = "rounding_mode", nullable = false, length = 30)
    private RoundingModeOption roundingMode = RoundingModeOption.HALF_UP;
}
