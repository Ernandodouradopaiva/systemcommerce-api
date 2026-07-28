package br.com.systemcommerce.pos.cash.entity;

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
@Table(name = "cash_movement_reasons")
public class CashMovementReason extends AuditableEntity {

    public enum AppliesTo {
        SUPPLY,
        WITHDRAWAL,
        BOTH
    }

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to", nullable = false, length = 20)
    private AppliesTo appliesTo;

    public boolean appliesTo(CashMovement.MovementType type) {
        if (appliesTo == AppliesTo.BOTH) {
            return type == CashMovement.MovementType.SUPPLY || type == CashMovement.MovementType.WITHDRAWAL;
        }
        if (appliesTo == AppliesTo.SUPPLY) {
            return type == CashMovement.MovementType.SUPPLY;
        }
        return type == CashMovement.MovementType.WITHDRAWAL;
    }
}
