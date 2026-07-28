package br.com.systemcommerce.purchasesuggestion.entity;

import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.supplier.entity.Supplier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "purchase_suggestion_items")
public class PurchaseSuggestionItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suggestion_id", nullable = false)
    private PurchaseSuggestion suggestion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "on_hand_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal onHandQty = BigDecimal.ZERO;

    @Column(name = "available_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal availableQty = BigDecimal.ZERO;

    @Column(name = "in_transit_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal inTransitQty = BigDecimal.ZERO;

    @Column(name = "open_po_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal openPoQty = BigDecimal.ZERO;

    @Column(name = "avg_daily_consumption", nullable = false, precision = 18, scale = 4)
    private BigDecimal avgDailyConsumption = BigDecimal.ZERO;

    @Column(name = "coverage_days", precision = 18, scale = 4)
    private BigDecimal coverageDays;

    @Column(name = "reorder_point", nullable = false, precision = 18, scale = 4)
    private BigDecimal reorderPoint = BigDecimal.ZERO;

    @Column(name = "max_stock", precision = 18, scale = 4)
    private BigDecimal maxStock;

    @Column(name = "suggested_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal suggestedQty = BigDecimal.ZERO;

    @Column(name = "confidence_level", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidenceLevel = BigDecimal.ZERO;

    @Column(name = "justification", nullable = false, length = 2000)
    private String justification;

    @Column(name = "parameters_used_json", columnDefinition = "TEXT")
    private String parametersUsedJson;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
