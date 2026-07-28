package br.com.systemcommerce.stocktransfer.entity;

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
@Table(name = "stock_transfer_items")
public class StockTransferItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private StockTransfer transfer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_requested", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantityRequested;

    @Column(name = "quantity_approved", precision = 19, scale = 3)
    private BigDecimal quantityApproved;

    @Column(name = "quantity_dispatched", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantityDispatched = BigDecimal.ZERO;

    @Column(name = "quantity_received", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantityReceived = BigDecimal.ZERO;

    @Column(name = "quantity_divergent", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantityDivergent = BigDecimal.ZERO;

    @Column(name = "observation", length = 500)
    private String observation;
}
