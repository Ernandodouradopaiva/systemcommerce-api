package br.com.systemcommerce.pricing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Snapshot imutável de uma promoção aplicada a uma venda/pedido/orçamento (Prompt 69). */
@Getter
@Setter
@Entity
@Table(name = "promotion_applications")
public class PromotionApplication {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "sales_order_id")
    private UUID salesOrderId;

    @Column(name = "quote_id")
    private UUID quoteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "benefit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal benefitAmount = BigDecimal.ZERO;

    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private Instant appliedAt;

    @Column(name = "applied_by")
    private UUID appliedBy;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (appliedAt == null) {
            appliedAt = Instant.now();
        }
    }
}
