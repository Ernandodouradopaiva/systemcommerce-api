package br.com.systemcommerce.picking.entity;

import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.salesorder.entity.SalesOrderItem;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "picking_order_items")
public class PickingOrderItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "picking_order_id", nullable = false)
    private PickingOrder pickingOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_item_id")
    private SalesOrderItem salesOrderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Localização física do item (tabela {@code storage_locations}). Sem entidade JPA dedicada ainda —
     * mantido como referência simples; a ordenação por código de localização usa consulta nativa
     * ({@code PickingOrderItemRepository}).
     */
    @Column(name = "storage_location_id")
    private UUID storageLocationId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "quantity_requested", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityRequested;

    @Column(name = "quantity_picked", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityPicked = BigDecimal.ZERO;

    @Column(name = "barcode_scanned", length = 80)
    private String barcodeScanned;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substitute_product_id")
    private Product substituteProduct;

    @Column(name = "notes", length = 1000)
    private String notes;

    public BigDecimal pending() {
        BigDecimal requested = quantityRequested != null ? quantityRequested : BigDecimal.ZERO;
        BigDecimal picked = quantityPicked != null ? quantityPicked : BigDecimal.ZERO;
        BigDecimal pending = requested.subtract(picked);
        return pending.compareTo(BigDecimal.ZERO) > 0 ? pending : BigDecimal.ZERO;
    }
}
