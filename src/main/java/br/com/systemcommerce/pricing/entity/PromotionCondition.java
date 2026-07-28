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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "promotion_conditions")
public class PromotionCondition extends AuditableEntity {

    public enum ConditionType {
        PRODUCT,
        CATEGORY,
        BRAND,
        MIN_QUANTITY,
        MIN_AMOUNT,
        CUSTOMER_GROUP,
        COUPON
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 40)
    private ConditionType conditionType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "min_quantity", precision = 18, scale = 4)
    private BigDecimal minQuantity;

    @Column(name = "min_amount", precision = 18, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;
}
