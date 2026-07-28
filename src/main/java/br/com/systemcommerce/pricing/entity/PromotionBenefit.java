package br.com.systemcommerce.pricing.entity;

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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "promotion_benefits")
public class PromotionBenefit extends AuditableEntity {

    public enum BenefitType {
        PERCENT_DISCOUNT,
        FIXED_DISCOUNT,
        PROMO_PRICE,
        BUY_X_PAY_Y
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "benefit_type", nullable = false, length = 40)
    private BenefitType benefitType;

    @Column(name = "percent_value", precision = 7, scale = 4)
    private BigDecimal percentValue;

    @Column(name = "fixed_value", precision = 18, scale = 2)
    private BigDecimal fixedValue;

    @Column(name = "promo_unit_price", precision = 18, scale = 4)
    private BigDecimal promoUnitPrice;

    @Column(name = "buy_quantity", precision = 18, scale = 4)
    private BigDecimal buyQuantity;

    @Column(name = "pay_quantity", precision = 18, scale = 4)
    private BigDecimal payQuantity;

    @Column(name = "max_benefit_amount", precision = 18, scale = 2)
    private BigDecimal maxBenefitAmount;
}
