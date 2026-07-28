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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "purchase_quotation_items")
public class PurchaseQuotationItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_quotation_id", nullable = false)
    private PurchaseQuotation purchaseQuotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_item_id")
    private PurchaseRequestItem purchaseRequestItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "quantity_selected", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantitySelected = BigDecimal.ZERO;

    public BigDecimal pendingSelection() {
        BigDecimal selected = quantitySelected != null ? quantitySelected : BigDecimal.ZERO;
        BigDecimal pending = quantity.subtract(selected);
        return pending.signum() < 0 ? BigDecimal.ZERO : pending;
    }
}
