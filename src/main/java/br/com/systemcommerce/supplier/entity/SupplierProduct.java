package br.com.systemcommerce.supplier.entity;

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

/** Vínculo fornecedor x produto (catálogo de fornecimento). */
@Getter
@Setter
@Entity
@Table(name = "supplier_products")
public class SupplierProduct extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "supplier_sku", length = 60)
    private String supplierSku;

    @Column(name = "last_purchase_price", precision = 18, scale = 4)
    private BigDecimal lastPurchasePrice;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;
}
