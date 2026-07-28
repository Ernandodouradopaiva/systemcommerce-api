package br.com.systemcommerce.finance.policy.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_charge_policies")
public class FinancialChargePolicy extends AuditableEntity {

    public enum InterestType {
        NONE,
        SIMPLE_DAILY
    }

    public enum PenaltyType {
        NONE,
        FIXED,
        PERCENT
    }

    public enum EarlyDiscountType {
        NONE,
        PERCENT
    }

    public enum RoundingModeType {
        HALF_UP,
        DOWN,
        UP,
        HALF_EVEN
    }

    public enum Status {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", nullable = false, length = 40)
    private InterestType interestType = InterestType.NONE;

    @Column(name = "interest_rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(name = "interest_grace_days", nullable = false)
    private Integer interestGraceDays = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", nullable = false, length = 40)
    private PenaltyType penaltyType = PenaltyType.NONE;

    @Column(name = "penalty_fixed_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal penaltyFixedAmount = BigDecimal.ZERO;

    @Column(name = "penalty_percent", nullable = false, precision = 18, scale = 6)
    private BigDecimal penaltyPercent = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "early_discount_type", nullable = false, length = 40)
    private EarlyDiscountType earlyDiscountType = EarlyDiscountType.NONE;

    @Column(name = "early_discount_percent", nullable = false, precision = 18, scale = 6)
    private BigDecimal earlyDiscountPercent = BigDecimal.ZERO;

    @Column(name = "early_discount_days", nullable = false)
    private Integer earlyDiscountDays = 0;

    @Column(name = "max_authorized_discount_percent", nullable = false, precision = 18, scale = 6)
    private BigDecimal maxAuthorizedDiscountPercent = BigDecimal.ZERO;

    @Column(name = "requires_discount_authorization", nullable = false)
    private Boolean requiresDiscountAuthorization = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "rounding_mode", nullable = false, length = 20)
    private RoundingModeType roundingMode = RoundingModeType.HALF_UP;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.ACTIVE;
}
