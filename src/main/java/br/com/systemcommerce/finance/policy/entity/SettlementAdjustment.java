package br.com.systemcommerce.finance.policy.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "settlement_adjustments")
public class SettlementAdjustment extends AuditableEntity {

    public enum SettlementType {
        PAYABLE,
        RECEIVABLE
    }

    public enum AdjustmentType {
        INTEREST,
        PENALTY,
        DISCOUNT,
        AUTHORIZED_DISCOUNT,
        FEE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_type", nullable = false, length = 40)
    private SettlementType settlementType;

    @Column(name = "settlement_id", nullable = false)
    private UUID settlementId;

    @Column(name = "installment_id")
    private UUID installmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 40)
    private AdjustmentType adjustmentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private FinancialChargePolicy policy;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    @Column(name = "calculated_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal calculatedAmount;

    @Column(name = "applied_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal appliedAmount;

    @Column(nullable = false)
    private Boolean authorized = Boolean.FALSE;

    @Column(name = "authorized_by")
    private UUID authorizedBy;

    @Column(length = 1000)
    private String notes;
}
