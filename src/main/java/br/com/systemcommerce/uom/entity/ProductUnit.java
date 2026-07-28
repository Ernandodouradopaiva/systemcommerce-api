package br.com.systemcommerce.uom.entity;

import br.com.systemcommerce.product.entity.Product;
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

/** Unidades de estoque/compra/venda de um produto (1 registro por produto). */
@Getter
@Setter
@Entity
@Table(name = "product_units")
public class ProductUnit extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_unit_id", nullable = false)
    private UnitOfMeasure stockUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_unit_id", nullable = false)
    private UnitOfMeasure purchaseUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_unit_id", nullable = false)
    private UnitOfMeasure salesUnit;

    @Column(name = "purchase_to_stock_factor", nullable = false, precision = 24, scale = 10)
    private BigDecimal purchaseToStockFactor = BigDecimal.ONE;

    @Column(name = "sales_to_stock_factor", nullable = false, precision = 24, scale = 10)
    private BigDecimal salesToStockFactor = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "rounding_mode", nullable = false, length = 30)
    private RoundingModeOption roundingMode = RoundingModeOption.HALF_UP;
}
