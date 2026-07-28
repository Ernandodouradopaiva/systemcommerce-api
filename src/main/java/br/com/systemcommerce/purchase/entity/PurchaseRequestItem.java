package br.com.systemcommerce.purchase.entity;

import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import br.com.systemcommerce.supplier.entity.Supplier;
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
@Table(name = "purchase_request_items")
public class PurchaseRequestItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    @Column(name = "quantity_requested", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityRequested;

    @Column(name = "quantity_approved", precision = 18, scale = 4)
    private BigDecimal quantityApproved;

    @Column(name = "quantity_converted", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityConverted = BigDecimal.ZERO;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "current_stock_info", precision = 18, scale = 4)
    private BigDecimal currentStockInfo;

    @Column(name = "minimum_stock", precision = 18, scale = 4)
    private BigDecimal minimumStock;

    @Column(name = "justification", length = 1000)
    private String justification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_supplier_id")
    private Supplier suggestedSupplier;

    /** Saldo pendente de conversão: aprovado (ou solicitado, se ainda não analisado) − convertido. */
    public BigDecimal pendingQuantity() {
        BigDecimal base = quantityApproved != null ? quantityApproved : quantityRequested;
        BigDecimal converted = quantityConverted != null ? quantityConverted : BigDecimal.ZERO;
        BigDecimal pending = base.subtract(converted);
        return pending.signum() < 0 ? BigDecimal.ZERO.setScale(4) : pending;
    }
}
