package br.com.systemcommerce.storeproduct.entity;

import br.com.systemcommerce.pos.store.entity.Store;
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
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "store_products")
public class StoreProduct extends AuditableEntity {

    public enum StoreProductStatus {
        ACTIVE,
        BLOCKED,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StoreProductStatus status = StoreProductStatus.ACTIVE;

    @Column(name = "allows_sale", nullable = false)
    private Boolean allowsSale = Boolean.TRUE;

    @Column(name = "allows_pos_sale", nullable = false)
    private Boolean allowsPosSale = Boolean.TRUE;

    @Column(name = "allows_erp_sale", nullable = false)
    private Boolean allowsErpSale = Boolean.TRUE;

    @Column(name = "local_internal_code", length = 60)
    private String localInternalCode;

    @Column(name = "local_barcode", length = 60)
    private String localBarcode;

    @Column(name = "local_default_price", precision = 19, scale = 2)
    private BigDecimal localDefaultPrice;

    @Column(name = "local_min_stock", precision = 19, scale = 3)
    private BigDecimal localMinStock;

    @Column(name = "local_max_stock", precision = 19, scale = 3)
    private BigDecimal localMaxStock;

    @Column(name = "allow_negative_stock", nullable = false)
    private Boolean allowNegativeStock = Boolean.FALSE;

    @Column(name = "physical_location", length = 120)
    private String physicalLocation;

    @Column(name = "aisle", length = 40)
    private String aisle;

    @Column(name = "shelf", length = 40)
    private String shelf;

    @Column(name = "display_position", length = 80)
    private String displayPosition;

    @Column(name = "commercialization_start")
    private LocalDate commercializationStart;

    @Column(name = "commercialization_end")
    private LocalDate commercializationEnd;

    @Column(name = "block_reason", length = 500)
    private String blockReason;

    public boolean isAllowsSale() {
        return Boolean.TRUE.equals(allowsSale);
    }

    public boolean isAllowsPosSale() {
        return Boolean.TRUE.equals(allowsPosSale);
    }

    public boolean isAllowsErpSale() {
        return Boolean.TRUE.equals(allowsErpSale);
    }

    public boolean isAllowNegativeStock() {
        return Boolean.TRUE.equals(allowNegativeStock);
    }

    public boolean isUsableForPricing() {
        return Boolean.TRUE.equals(getActive()) && status == StoreProductStatus.ACTIVE;
    }
}
