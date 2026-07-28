package br.com.systemcommerce.pricing.entity;

import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "promotion_products")
public class PromotionProduct extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "promotional_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal promotionalPrice;

    @Column(name = "min_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal minQuantity = BigDecimal.ONE;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive());
    }

    public boolean meetsMinQuantity(BigDecimal quantity) {
        BigDecimal min = minQuantity != null ? minQuantity : BigDecimal.ONE;
        return quantity != null && quantity.compareTo(min) >= 0;
    }
}
