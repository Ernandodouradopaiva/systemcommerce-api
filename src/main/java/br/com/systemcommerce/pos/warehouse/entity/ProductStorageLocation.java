package br.com.systemcommerce.pos.warehouse.entity;

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
@Table(name = "product_storage_locations")
public class ProductStorageLocation extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "storage_location_id", nullable = false)
    private StorageLocation storageLocation;

    @Column(name = "preferred", nullable = false)
    private Boolean preferred = Boolean.FALSE;

    @Column(name = "min_quantity", precision = 18, scale = 4)
    private BigDecimal minQuantity;

    @Column(name = "max_quantity", precision = 18, scale = 4)
    private BigDecimal maxQuantity;

    @Column(name = "quantity_at_location", precision = 18, scale = 4)
    private BigDecimal quantityAtLocation;
}
