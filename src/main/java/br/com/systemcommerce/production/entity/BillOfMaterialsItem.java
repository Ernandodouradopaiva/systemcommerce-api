package br.com.systemcommerce.production.entity;

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
@Table(name = "bill_of_materials_items")
public class BillOfMaterialsItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_of_materials_id", nullable = false)
    private BillOfMaterials billOfMaterials;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_product_id", nullable = false)
    private Product componentProduct;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_code", length = 20)
    private String unitCode;

    @Column(name = "scrap_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal scrapPercent = BigDecimal.ZERO;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;
}
