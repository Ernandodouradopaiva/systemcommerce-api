package br.com.systemcommerce.salesorder.entity;

import br.com.systemcommerce.pos.store.entity.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "store_sales_order_sequences")
public class StoreSalesOrderSequence {

    @Id
    @Column(name = "store_id", nullable = false, updatable = false)
    private UUID storeId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "last_value", nullable = false)
    private Long lastValue = 0L;

    @Column(name = "prefix", nullable = false, length = 10)
    private String prefix = "P";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onPrePersist() {
        if (lastValue == null) {
            lastValue = 0L;
        }
        if (prefix == null || prefix.isBlank()) {
            prefix = "P";
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    public long incrementAndGet() {
        long next = lastValue + 1;
        lastValue = next;
        updatedAt = Instant.now();
        return next;
    }
}
