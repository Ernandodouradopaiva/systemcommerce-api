package br.com.systemcommerce.bundle.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "product_bundles")
public class ProductBundle extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "bundle_type", nullable = false, length = 40)
    private ProductBundleType bundleType;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_policy", nullable = false, length = 40)
    private BundlePricePolicyType pricePolicy = BundlePricePolicyType.FIXED;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_policy", nullable = false, length = 40)
    private BundleInventoryPolicyType inventoryPolicy = BundleInventoryPolicyType.COMPONENTS;

    @Column(name = "fixed_price", precision = 19, scale = 2)
    private BigDecimal fixedPrice;

    @Column(name = "component_discount_pct", precision = 7, scale = 4)
    private BigDecimal componentDiscountPct;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductBundleStatus status = ProductBundleStatus.ACTIVE;

    @OneToMany(mappedBy = "productBundle")
    @OrderBy("lineNumber ASC")
    private List<ProductBundleItem> items = new ArrayList<>();
}
