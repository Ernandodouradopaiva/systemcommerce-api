package br.com.systemcommerce.pricing.entity;

import br.com.systemcommerce.catalog.entity.Brand;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "promotions")
public class Promotion extends AuditableEntity {

    public enum Status {
        ACTIVE,
        INACTIVE
    }

    /** Tipo de motor de promoção (Prompt 69). Nulo mantém compatibilidade com promoções simples por produto. */
    public enum PromotionType {
        PERCENT_DISCOUNT,
        FIXED_DISCOUNT,
        PROMO_PRICE,
        BUY_QTY_DISCOUNT,
        BUY_X_PAY_Y,
        MIN_AMOUNT,
        COUPON
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private PriceChannel channel = PriceChannel.POS;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "priority", nullable = false)
    private Integer priority = 100;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "promotion_stores",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "store_id"))
    private Set<Store> stores = new HashSet<>();

    @OneToMany(mappedBy = "promotion", fetch = FetchType.LAZY)
    private Set<PromotionProduct> products = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", length = 40)
    private PromotionType promotionType;

    @Column(name = "stackable", nullable = false)
    private boolean stackable = false;

    @Column(name = "min_order_amount", precision = 18, scale = 2)
    private BigDecimal minOrderAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "promotion", fetch = FetchType.LAZY)
    private Set<PromotionRule> rules = new HashSet<>();

    @OneToMany(mappedBy = "promotion", fetch = FetchType.LAZY)
    private Set<PromotionCondition> conditions = new HashSet<>();

    @OneToMany(mappedBy = "promotion", fetch = FetchType.LAZY)
    private Set<PromotionBenefit> benefits = new HashSet<>();

    public boolean isEngineBased() {
        return promotionType != null;
    }

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == Status.ACTIVE;
    }

    public boolean isValidAt(Instant at) {
        if (validFrom != null && at.isBefore(validFrom)) {
            return false;
        }
        return validTo == null || !at.isAfter(validTo);
    }
}
