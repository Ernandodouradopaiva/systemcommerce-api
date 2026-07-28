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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "promotion_rules")
public class PromotionRule extends AuditableEntity {

    public enum RuleType {
        PERCENT_DISCOUNT,
        FIXED_DISCOUNT,
        PROMO_PRICE,
        BUY_QTY_DISCOUNT,
        BUY_X_PAY_Y,
        CATEGORY,
        BRAND,
        MIN_AMOUNT,
        CUSTOMER_GROUP,
        COUPON
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 40)
    private RuleType ruleType;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
