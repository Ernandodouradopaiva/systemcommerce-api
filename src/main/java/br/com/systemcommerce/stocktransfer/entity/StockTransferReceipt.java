package br.com.systemcommerce.stocktransfer.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import br.com.systemcommerce.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stock_transfer_receipts")
public class StockTransferReceipt extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private StockTransfer transfer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private StockTransferItem item;

    @Column(name = "quantity_received", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantityReceived;

    @Column(name = "quantity_expected", precision = 19, scale = 3)
    private BigDecimal quantityExpected;

    @Column(name = "divergence_quantity", nullable = false, precision = 19, scale = 3)
    private BigDecimal divergenceQuantity = BigDecimal.ZERO;

    @Column(name = "divergence_reason", length = 500)
    private String divergenceReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private User receivedBy;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;
}
