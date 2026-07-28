package br.com.systemcommerce.purchasesuggestion.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
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
@Table(name = "purchase_suggestion_parameters")
public class PurchaseSuggestionParameter extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "default_lead_time_days", nullable = false)
    private Integer defaultLeadTimeDays = 7;

    @Column(name = "safety_stock_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal safetyStockDays = new BigDecimal("3");

    @Column(name = "seasonality_factor", nullable = false, precision = 8, scale = 4)
    private BigDecimal seasonalityFactor = BigDecimal.ONE;

    @Column(name = "min_purchase_multiple", nullable = false, precision = 18, scale = 4)
    private BigDecimal minPurchaseMultiple = BigDecimal.ONE;

    @Column(name = "min_lot_size", nullable = false, precision = 18, scale = 4)
    private BigDecimal minLotSize = BigDecimal.ONE;

    @Column(name = "coverage_target_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal coverageTargetDays = new BigDecimal("14");
}
