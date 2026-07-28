package br.com.systemcommerce.reservation.entity;

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
@Table(name = "stock_reservation_items")
public class StockReservationItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_reservation_id", nullable = false)
    private StockReservation stockReservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "quantity_reserved", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityReserved;

    @Column(name = "quantity_consumed", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityConsumed = BigDecimal.ZERO;

    @Column(name = "quantity_released", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityReleased = BigDecimal.ZERO;

    /** Quantidade ainda ativa (reservada e não consumida/liberada) neste item. */
    public BigDecimal remaining() {
        BigDecimal reserved = quantityReserved != null ? quantityReserved : BigDecimal.ZERO;
        BigDecimal consumed = quantityConsumed != null ? quantityConsumed : BigDecimal.ZERO;
        BigDecimal released = quantityReleased != null ? quantityReleased : BigDecimal.ZERO;
        return reserved.subtract(consumed).subtract(released);
    }
}
