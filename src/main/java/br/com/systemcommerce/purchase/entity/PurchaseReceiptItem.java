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
@Table(name = "purchase_receipt_items")
public class PurchaseReceiptItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_receipt_id", nullable = false)
    private PurchaseReceipt purchaseReceipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_item_id", nullable = false)
    private PurchaseOrderItem purchaseOrderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_ordered", precision = 18, scale = 4)
    private BigDecimal quantityOrdered;

    @Column(name = "quantity_previously_received", precision = 18, scale = 4)
    private BigDecimal quantityPreviouslyReceived;

    @Column(name = "quantity_received", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityReceived;

    @Column(name = "quantity_rejected", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityRejected = BigDecimal.ZERO;

    @Column(name = "quantity_accepted", precision = 18, scale = 4)
    private BigDecimal quantityAccepted;

    @Column(name = "quantity_divergent", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityDivergent = BigDecimal.ZERO;

    @Column(name = "unit_cost", precision = 18, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "batch_code", length = 60)
    private String batchCode;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "serial_number", length = 120)
    private String serialNumber;

    @Column(name = "destination_location", length = 120)
    private String destinationLocation;

    /** Quantidade efetivamente aceita para entrada em estoque (fallback: recebida, se não inspecionada). */
    public BigDecimal effectiveAcceptedQuantity() {
        return quantityAccepted != null ? quantityAccepted : quantityReceived;
    }
}
