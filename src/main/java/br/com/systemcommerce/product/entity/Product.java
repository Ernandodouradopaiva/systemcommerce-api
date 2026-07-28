package br.com.systemcommerce.product.entity;

import br.com.systemcommerce.catalog.entity.Brand;
import br.com.systemcommerce.catalog.entity.Manufacturer;
import br.com.systemcommerce.catalog.entity.ProductLine;
import br.com.systemcommerce.organization.entity.Organization;
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

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product extends AuditableEntity {

    public enum ProductStatus {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "internal_code", nullable = false, unique = true, length = 60)
    private String internalCode;

    @Column(name = "sku", nullable = false, unique = true, length = 60)
    private String sku;

    @Column(name = "barcode", length = 60)
    private String barcode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure = "UN";

    /** Preço de venda. */
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal salePrice = BigDecimal.ZERO;

    /** Preço de custo. */
    @Column(name = "cost_price", precision = 19, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "min_stock", nullable = false, precision = 19, scale = 3)
    private BigDecimal minStock = BigDecimal.ZERO;

    @Column(name = "allow_negative_stock", nullable = false)
    private Boolean allowNegativeStock = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id")
    private Manufacturer manufacturer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_line_id")
    private ProductLine productLine;

    @Column(name = "requires_batch", nullable = false)
    private Boolean requiresBatch = Boolean.FALSE;

    @Column(name = "fefo_enabled", nullable = false)
    private Boolean fefoEnabled = Boolean.FALSE;

    @Column(name = "requires_serial", nullable = false)
    private Boolean requiresSerial = Boolean.FALSE;

    public boolean isUsableForSale() {
        return Boolean.TRUE.equals(getActive()) && status == ProductStatus.ACTIVE;
    }

    public void markActive() {
        this.status = ProductStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = ProductStatus.INACTIVE;
        setActive(false);
    }
}
