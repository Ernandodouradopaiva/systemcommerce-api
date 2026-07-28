package br.com.systemcommerce.pricing.entity;

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

/** Faixa de preço por quantidade (ex.: 1 CX = 12 UN a partir de 5 CX sai por X) — Prompt 68. */
@Getter
@Setter
@Entity
@Table(name = "price_tiers")
public class PriceTier extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_price_id", nullable = false)
    private ProductPrice productPrice;

    @Column(name = "min_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal minQuantity;

    @Column(name = "max_quantity", precision = 18, scale = 4)
    private BigDecimal maxQuantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    public boolean matches(BigDecimal quantity) {
        if (quantity == null) {
            return false;
        }
        if (quantity.compareTo(minQuantity) < 0) {
            return false;
        }
        return maxQuantity == null || quantity.compareTo(maxQuantity) <= 0;
    }
}
