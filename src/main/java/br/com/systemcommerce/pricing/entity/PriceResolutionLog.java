package br.com.systemcommerce.pricing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Registro de auditoria de cada resolução de preço (Prompt 68) — apenas leitura/histórico, sem regra própria. */
@Getter
@Setter
@Entity
@Table(name = "price_resolution_logs")
public class PriceResolutionLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "channel", length = 30)
    private String channel;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "quantity", precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "resolved_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal resolvedPrice;

    @Column(name = "price_origin", nullable = false, length = 80)
    private String priceOrigin;

    @Column(name = "price_table_id")
    private UUID priceTableId;

    @Column(name = "product_price_id")
    private UUID productPriceId;

    @Column(name = "resolved_at", nullable = false, updatable = false)
    private Instant resolvedAt;

    @Column(name = "context_json", columnDefinition = "TEXT")
    private String contextJson;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (resolvedAt == null) {
            resolvedAt = Instant.now();
        }
    }
}
