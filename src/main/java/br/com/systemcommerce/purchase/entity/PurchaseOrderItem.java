package br.com.systemcommerce.purchase.entity;

import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "purchase_order_items")
public class PurchaseOrderItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "quantity_ordered", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityOrdered;

    @Column(name = "quantity_received", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityReceived = BigDecimal.ZERO;

    @Column(name = "quantity_cancelled", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityCancelled = BigDecimal.ZERO;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "historical_description", length = 300)
    private String historicalDescription;

    @Column(name = "unit_cost", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    public BigDecimal remainingQuantity() {
        BigDecimal ordered = quantityOrdered != null ? quantityOrdered : BigDecimal.ZERO;
        BigDecimal received = quantityReceived != null ? quantityReceived : BigDecimal.ZERO;
        return ordered.subtract(received);
    }
}
