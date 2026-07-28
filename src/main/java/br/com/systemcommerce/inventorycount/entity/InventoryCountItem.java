package br.com.systemcommerce.inventorycount.entity;

import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import br.com.systemcommerce.pos.warehouse.entity.StorageLocation;
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
@Table(name = "inventory_count_items")
public class InventoryCountItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_count_id", nullable = false)
    private InventoryCount inventoryCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_location_id")
    private StorageLocation storageLocation;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "theoretical_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal theoreticalQuantity = BigDecimal.ZERO;

    @Column(name = "counted_quantity_1", precision = 18, scale = 4)
    private BigDecimal countedQuantity1;

    @Column(name = "counted_quantity_2", precision = 18, scale = 4)
    private BigDecimal countedQuantity2;

    @Column(name = "final_counted_quantity", precision = 18, scale = 4)
    private BigDecimal finalCountedQuantity;

    @Column(name = "variance_quantity", precision = 18, scale = 4)
    private BigDecimal varianceQuantity;

    @Column(name = "unit_cost", precision = 18, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "frozen", nullable = false)
    private Boolean frozen = Boolean.FALSE;

    @Column(name = "notes", length = 1000)
    private String notes;
}
