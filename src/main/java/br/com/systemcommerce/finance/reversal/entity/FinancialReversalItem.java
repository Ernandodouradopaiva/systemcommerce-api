package br.com.systemcommerce.finance.reversal.entity;

import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_reversal_items")
public class FinancialReversalItem {

    public enum ItemType {
        HOLDER_MOVEMENT,
        INSTALLMENT
    }

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reversal_id", nullable = false)
    private FinancialReversal reversal;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 40)
    private ItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_movement_id")
    private FinancialHolderMovement originalMovement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_movement_id")
    private FinancialHolderMovement reversalMovement;

    @Column(name = "original_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "reversed_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal reversedAmount;

    @Column(name = "target_installment_id")
    private UUID targetInstallmentId;

    @Column(length = 1000)
    private String notes;

    @PrePersist
    void pre() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
